package com.ecoanalytics.infraestructura.simulacion;

import com.ecoanalytics.model.*;
import com.ecoanalytics.model.evento.MedicionesRegistradasEvento;
import com.ecoanalytics.repository.EstacionRepository;
import com.ecoanalytics.repository.InsercionMasiva;
import com.ecoanalytics.repository.MedicionRepository;
import com.ecoanalytics.service.IndicadorService;
import com.ecoanalytics.service.PronosticoService;
import com.ecoanalytics.service.RegistroTareas;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tareas en segundo plano: un simulador de sensores IoT mantiene frescas las condiciones actuales, los
 * indicadores se agregan cada hora y los pronósticos se refrescan para que puedan dispararse alertas predictivas.
 */
@Component
public class TareasProgramadas {

    static final String SIMULADOR_SENSORES = "Simulador de sensores IoT";
    static final String INDICADORES = "Agregación de indicadores";
    static final String PRONOSTICOS = "Refresco de pronósticos ML";
    private static final int MAX_HORAS_RELLENO = 48;

    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final InsercionMasiva insercion;
    private final GeneradorCalidadAireSintetico generador;
    private final IndicadorService indicadorService;
    private final PronosticoService pronosticoService;
    private final ApplicationEventPublisher eventos;
    private final RegistroTareas registro;

    public TareasProgramadas(EstacionRepository estaciones, MedicionRepository mediciones, InsercionMasiva insercion,
                             GeneradorCalidadAireSintetico generador, IndicadorService indicadorService,
                             PronosticoService pronosticoService, ApplicationEventPublisher eventos,
                             RegistroTareas registro) {
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.insercion = insercion;
        this.generador = generador;
        this.indicadorService = indicadorService;
        this.pronosticoService = pronosticoService;
        this.eventos = eventos;
        this.registro = registro;
        registro.registrar(SIMULADOR_SENSORES, "Ingesta horaria de estaciones en línea", "Cada 5 minutos");
        registro.registrar(INDICADORES, "Promedios móviles, excedencias y variaciones", "Cada hora (min. 05)");
        registro.registrar(PRONOSTICOS, "Inferencia 48 h y evaluación de alertas predictivas", "Cada 30 minutos");
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
    public void simularSensores() {
        ejecutar(SIMULADOR_SENSORES, () -> {
            LocalDateTime horaActual = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
            LocalDateTime limiteRelleno = horaActual.minusHours(MAX_HORAS_RELLENO);
            List<Medicion> nuevas = new ArrayList<>();
            for (Estacion estacion : estaciones.findByEstado(EstadoEstacion.EN_LINEA)) {
                LocalDateTime ultima = mediciones.findFirstByEstacionIdOrderByFechaMedicionDesc(estacion.getId())
                        .map(Medicion::getFechaMedicion).orElse(limiteRelleno);
                LocalDateTime siguiente = ultima.truncatedTo(ChronoUnit.HOURS).plusHours(1);
                if (siguiente.isBefore(limiteRelleno)) siguiente = limiteRelleno;
                for (; !siguiente.isAfter(horaActual); siguiente = siguiente.plusHours(1)) {
                    GeneradorCalidadAireSintetico.Lectura l = generador.siguiente(estacion.getNombre(), siguiente);
                    nuevas.add(new Medicion(estacion.getId(), siguiente, l.pm25(), l.pm10(), l.temperatura(), l.humedad(),
                            TipoFuente.SENSOR, EstadoMedicion.VALIDA, null));
                }
            }
            List<Long> ids = insercion.insertar(nuevas, Medicion.class).stream().map(Medicion::getId).toList();
            if (!ids.isEmpty()) eventos.publishEvent(new MedicionesRegistradasEvento(ids));
            return ids.size() + " lecturas nuevas";
        });
    }

    @Scheduled(cron = "0 5 * * * *")
    public void agregarIndicadores() {
        ejecutar(INDICADORES, () -> {
            IndicadorService.ResultadoRecalculo r = indicadorService.recalcular();
            return r.indicadores() + " indicadores sobre " + r.registrosProcesados() + " registros";
        });
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT30M")
    public void refrescarPronosticos() {
        ejecutar(PRONOSTICOS, () -> {
            int correctos = 0;
            for (Estacion estacion : estaciones.findByEstado(EstadoEstacion.EN_LINEA)) {
                try {
                    pronosticoService.pronosticar(estacion.getId(), 24);
                    correctos++;
                } catch (RuntimeException sinDatos) {
                    // Una estación sin datos recientes suficientes simplemente no tiene pronóstico en este ciclo.
                }
            }
            return correctos + " estaciones pronosticadas";
        });
    }

    private void ejecutar(String tarea, Supplier<String> trabajo) {
        long inicio = System.currentTimeMillis();
        try {
            registro.exito(tarea, System.currentTimeMillis() - inicio, trabajo.get());
        } catch (RuntimeException e) {
            registro.fallo(tarea, System.currentTimeMillis() - inicio, e.getMessage());
        }
    }
}
