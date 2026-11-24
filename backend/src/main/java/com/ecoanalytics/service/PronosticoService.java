package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.CategoriaIca;
import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.model.Estadistica;
import com.ecoanalytics.model.IndiceCalidadAire;
import com.ecoanalytics.model.VersionModelo;
import com.ecoanalytics.model.evento.PronosticoGeneradoEvento;
import com.ecoanalytics.model.prediccion.ConstructorCaracteristicas;
import com.ecoanalytics.model.prediccion.ConstructorCaracteristicas.PuntoHorario;
import com.ecoanalytics.model.prediccion.ModeloPredictivo;
import com.ecoanalytics.model.prediccion.Pronostico;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pronóstico recursivo de varios pasos (HU-13): cada hora predicha alimenta los rezagos de la siguiente.
 * La incertidumbre crece con el horizonte a partir del RMSE de prueba del propio modelo.
 */
@Service
public class PronosticoService {

    private static final Duration VIGENCIA_CACHE = Duration.ofMinutes(15);
    private static final int HORAS_HISTORIA = 48;
    private static final int HORIZONTE_MAXIMO = 48;

    private final EntrenamientoModeloService entrenamiento;
    private final EstacionService estacionService;
    private final ApplicationEventPublisher eventos;
    private final Map<String, Pronostico> cache = new ConcurrentHashMap<>();

    public PronosticoService(EntrenamientoModeloService entrenamiento, EstacionService estacionService,
                             ApplicationEventPublisher eventos) {
        this.entrenamiento = entrenamiento;
        this.estacionService = estacionService;
        this.eventos = eventos;
    }

    public record VistaPronostico(Pronostico pronostico, int horizonteHoras, int icaProyectado,
                                  CategoriaIca categoriaProyectada, String recomendacion, Pronostico.Punto pico,
                                  double limiteOms, int horasSobreOms) {
    }

    public VistaPronostico pronosticar(Long estacionId, int horas) {
        if (horas != 24 && horas != 48) throw ExcepcionNegocio.invalido("El horizonte debe ser de 24 o 48 horas.");
        Pronostico pronostico = enCacheOCalcular(estacionId).recortado(horas);
        List<Pronostico.Punto> puntos = pronostico.puntos();

        double media = puntos.stream().mapToDouble(Pronostico.Punto::pm25).average().orElse(0);
        IndiceCalidadAire proyectado = IndiceCalidadAire.desdePm25(media);
        Pronostico.Punto pico = pronostico.pico();
        CategoriaIca peor = pico.ica().categoria().ordinal() > proyectado.categoria().ordinal()
                ? pico.ica().categoria() : proyectado.categoria();
        int sobreOms = (int) puntos.stream().filter(p -> p.pm25() > IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25).count();
        return new VistaPronostico(pronostico, horas, proyectado.valor(), proyectado.categoria(), peor.recomendacionSalud(),
                pico, IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25, sobreOms);
    }

    private Pronostico enCacheOCalcular(Long estacionId) {
        VersionModelo produccion = entrenamiento.enProduccion();
        String clave = estacionId + ":" + produccion.getVersion();
        Pronostico guardado = cache.get(clave);
        if (guardado != null && guardado.fechaGeneracion().isAfter(LocalDateTime.now().minus(VIGENCIA_CACHE))) {
            return guardado;
        }
        Pronostico nuevo = calcular(estacionId, produccion);
        cache.put(clave, nuevo);
        eventos.publishEvent(new PronosticoGeneradoEvento(nuevo));
        return nuevo;
    }

    private Pronostico calcular(Long estacionId, VersionModelo produccion) {
        Estacion estacion = estacionService.buscar(estacionId);
        if (!estacion.estaTransmitiendo()) {
            throw ExcepcionNegocio.invalido("Predicción no disponible: la estación " + estacion.getNombre()
                    + " no está transmitiendo datos actualmente.");
        }
        ModeloPredictivo modelo = entrenamiento.asegurarAjustado(produccion);
        LocalDateTime ahora = LocalDateTime.now();
        List<PuntoHorario> crudo = entrenamiento.serieHoraria(estacionId, ahora.minusHours(HORAS_HISTORIA + 24L), ahora);
        if (crudo.size() < ConstructorCaracteristicas.HISTORIA_REQUERIDA + 6
                || crudo.getLast().fecha().isBefore(ahora.minusHours(6))) {
            throw ExcepcionNegocio.invalido("Predicción no disponible: cantidad insuficiente de datos recientes para "
                    + estacion.getNombre() + ".");
        }
        List<PuntoHorario> serie = regularizar(crudo);
        double[][] climatologia = climatologiaHoraria(crudo);

        List<Pronostico.Punto> puntos = new ArrayList<>();
        for (int h = 1; h <= HORIZONTE_MAXIMO; h++) {
            LocalDateTime fecha = serie.getLast().fecha().plusHours(1);
            double temperatura = climatologia[fecha.getHour()][0];
            double humedad = climatologia[fecha.getHour()][1];
            double valor = Math.max(0, modelo.predecir(
                    ConstructorCaracteristicas.caracteristicas(serie, serie.size(), temperatura, humedad)));
            double semiancho = 1.96 * produccion.getRmse() * Math.sqrt(1 + (h - 1) / 6.0);
            double pm25 = Estadistica.redondear(valor);
            puntos.add(new Pronostico.Punto(fecha, pm25, Estadistica.redondear(Math.max(0, valor - semiancho)),
                    Estadistica.redondear(valor + semiancho), IndiceCalidadAire.desdePm25(pm25)));
            serie.add(new PuntoHorario(fecha, valor, temperatura, humedad));
        }
        return new Pronostico(estacion.getId(), estacion.getNombre(), produccion.getVersion(), produccion.getAlgoritmo(),
                produccion.getRmse(), ahora, puntos);
    }

    /** Completa horas faltantes repitiendo la lectura anterior, para que los rezagos sean siempre horarios. */
    private static List<PuntoHorario> regularizar(List<PuntoHorario> crudo) {
        List<PuntoHorario> serie = new ArrayList<>();
        PuntoHorario anterior = crudo.getFirst();
        serie.add(anterior);
        for (int i = 1; i < crudo.size(); i++) {
            PuntoHorario p = crudo.get(i);
            LocalDateTime esperada = anterior.fecha().plusHours(1);
            while (esperada.isBefore(p.fecha())) {
                serie.add(new PuntoHorario(esperada, anterior.pm25(), anterior.temperatura(), anterior.humedad()));
                esperada = esperada.plusHours(1);
            }
            if (p.fecha().isAfter(anterior.fecha())) {
                serie.add(p);
                anterior = p;
            }
        }
        return serie;
    }

    /** Temperatura y humedad promedio por hora del día: la estimación usada para las horas futuras. */
    private static double[][] climatologiaHoraria(List<PuntoHorario> historia) {
        double[][] sumas = new double[24][2];
        int[] conteos = new int[24];
        for (PuntoHorario p : historia) {
            int h = p.fecha().getHour();
            sumas[h][0] += p.temperatura();
            sumas[h][1] += p.humedad();
            conteos[h]++;
        }
        double mediaT = historia.stream().mapToDouble(PuntoHorario::temperatura).average().orElse(18);
        double mediaH = historia.stream().mapToDouble(PuntoHorario::humedad).average().orElse(70);
        double[][] resultado = new double[24][2];
        for (int h = 0; h < 24; h++) {
            resultado[h][0] = conteos[h] == 0 ? mediaT : sumas[h][0] / conteos[h];
            resultado[h][1] = conteos[h] == 0 ? mediaH : sumas[h][1] / conteos[h];
        }
        return resultado;
    }
}
