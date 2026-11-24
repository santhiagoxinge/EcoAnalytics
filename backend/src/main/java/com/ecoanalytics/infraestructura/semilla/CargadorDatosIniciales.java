package com.ecoanalytics.infraestructura.semilla;

import com.ecoanalytics.infraestructura.simulacion.GeneradorCalidadAireSintetico;
import com.ecoanalytics.model.*;
import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.repository.*;
import com.ecoanalytics.service.AlertaService;
import com.ecoanalytics.service.AnomaliaService;
import com.ecoanalytics.service.EntrenamientoModeloService;
import com.ecoanalytics.service.IndicadorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Deja lista una demostración en el primer arranque: usuarios de cada rol, la red de estaciones, más de un año
 * de histórico horario, modelos entrenados e indicadores calculados. Cada paso solo se ejecuta si sus datos
 * faltan, así que reiniciar la aplicación no duplica nada.
 */
@Component
public class CargadorDatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorDatosIniciales.class);
    private static final int TAMANO_BLOQUE = 5000;

    private final UsuarioRepository usuarios;
    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final CanalAlertaRepository canales;
    private final ConfiguracionIndicadorRepository configuracionIndicadores;
    private final ConfiguracionSistemaRepository configuracionSistema;
    private final VersionModeloRepository versionesModelo;
    private final InsercionMasiva insercion;
    private final PasswordEncoder cifrador;
    private final GeneradorCalidadAireSintetico generador;
    private final AnomaliaService anomaliaService;
    private final AlertaService alertaService;
    private final EntrenamientoModeloService entrenamientoService;
    private final IndicadorService indicadorService;
    private final int diasHistorico;

    public CargadorDatosIniciales(UsuarioRepository usuarios, EstacionRepository estaciones, MedicionRepository mediciones,
                                  CanalAlertaRepository canales, ConfiguracionIndicadorRepository configuracionIndicadores,
                                  ConfiguracionSistemaRepository configuracionSistema, VersionModeloRepository versionesModelo,
                                  InsercionMasiva insercion, PasswordEncoder cifrador, GeneradorCalidadAireSintetico generador,
                                  AnomaliaService anomaliaService, AlertaService alertaService,
                                  EntrenamientoModeloService entrenamientoService, IndicadorService indicadorService,
                                  @Value("${ecoanalytics.seed.history-days}") int diasHistorico) {
        this.usuarios = usuarios;
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.canales = canales;
        this.configuracionIndicadores = configuracionIndicadores;
        this.configuracionSistema = configuracionSistema;
        this.versionesModelo = versionesModelo;
        this.insercion = insercion;
        this.cifrador = cifrador;
        this.generador = generador;
        this.anomaliaService = anomaliaService;
        this.alertaService = alertaService;
        this.entrenamientoService = entrenamientoService;
        this.indicadorService = indicadorService;
        this.diasHistorico = diasHistorico;
    }

    @Override
    public void run(ApplicationArguments args) {
        cargarUsuarios();
        cargarEstaciones();
        cargarConfiguracion();
        if (mediciones.count() == 0) {
            cargarHistorico();
            detectarEventosRecientes();
        }
        if (versionesModelo.count() == 0) entrenarModelosIniciales();
        indicadorService.recalcular();
        log.info("EcoAnalytics listo: {} usuarios, {} estaciones, {} mediciones, {} modelos",
                usuarios.count(), estaciones.count(), mediciones.count(), versionesModelo.count());
    }

    private void cargarUsuarios() {
        if (usuarios.count() > 0) return;
        usuario("Administrador EcoAnalytics", "admin@ecoanalytics.org", "Admin123!", Rol.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        usuario("Laura Restrepo", "analista@ecoanalytics.org", "Analista123!", Rol.ANALISTA, EstadoUsuario.ACTIVO);
        usuario("Carlos Mendoza", "ciudadano@ecoanalytics.org", "Ciudadano123!", Rol.CIUDADANO, EstadoUsuario.ACTIVO);
        usuario("Ana García", "ana.garcia@ecoanalytics.org", "Ciudadano123!", Rol.CIUDADANO, EstadoUsuario.ACTIVO);
        usuario("Carlos López", "carlos.lopez@ecoanalytics.org", "Analista123!", Rol.ANALISTA, EstadoUsuario.ACTIVO);
        usuario("Mariana Ortiz", "mariana.ortiz@udenar.edu.co", "Analista123!", Rol.ANALISTA, EstadoUsuario.PENDIENTE);
        usuario("Jorge Patiño", "jorge.patino@ecoanalytics.org", "Ciudadano123!", Rol.CIUDADANO, EstadoUsuario.INACTIVO);
    }

    private void usuario(String nombre, String correo, String contrasena, Rol rol, EstadoUsuario estado) {
        usuarios.save(new Usuario(nombre, correo, cifrador.encode(contrasena), rol, estado));
    }

    private void cargarEstaciones() {
        if (estaciones.count() > 0) return;
        String todas = "PM2.5, PM10, Temp, Humedad";
        estaciones.save(new Estacion("Estación Centro", "Comuna 10 - La Candelaria", 6.2518, -75.5636, TipoFuente.SENSOR, todas));
        estaciones.save(new Estacion("Estación Parque Norte", "Comuna 4 - Aranjuez", 6.2710, -75.5650, TipoFuente.API, todas));
        Estacion belen = new Estacion("Estación Belén Alta", "Comuna 16 - Belén", 6.2310, -75.6030, TipoFuente.SENSOR, todas);
        belen.cambiarEstado(EstadoEstacion.MANTENIMIENTO);
        estaciones.save(belen);
        estaciones.save(new Estacion("Estación Poblado Sur", "Comuna 14 - El Poblado", 6.2088, -75.5680, TipoFuente.CSV, todas));
        Estacion robledo = new Estacion("Estación Robledo", "Comuna 7 - Robledo", 6.2800, -75.5950, TipoFuente.SENSOR, "PM2.5, PM10");
        robledo.cambiarEstado(EstadoEstacion.FUERA_DE_LINEA);
        estaciones.save(robledo);
    }

    private void cargarConfiguracion() {
        for (CanalAlerta.Codigo codigo : CanalAlerta.Codigo.values()) {
            if (!canales.existsById(codigo.name())) canales.save(new CanalAlerta(codigo, true));
        }
        for (TipoIndicador tipo : TipoIndicador.values()) {
            if (!configuracionIndicadores.existsById(tipo.name())) configuracionIndicadores.save(new ConfiguracionIndicador(tipo));
        }
        if (!configuracionSistema.existsById(ConfiguracionSistema.UMBRAL_Z_ANOMALIAS)) {
            configuracionSistema.save(new ConfiguracionSistema(ConfiguracionSistema.UMBRAL_Z_ANOMALIAS, "3.0"));
        }
        if (!configuracionSistema.existsById(ConfiguracionSistema.ALERTAS_PREDICTIVAS_HABILITADAS)) {
            configuracionSistema.save(new ConfiguracionSistema(ConfiguracionSistema.ALERTAS_PREDICTIVAS_HABILITADAS, "true"));
        }
    }

    /** Fila del histórico antes de convertirse en Medicion; mutable para poder inyectar episodios. */
    private static final class Fila {
        final LocalDateTime fecha;
        Double pm25, pm10, temperatura, humedad;
        EstadoMedicion estado = EstadoMedicion.VALIDA;

        Fila(LocalDateTime fecha) {
            this.fecha = fecha;
        }
    }

    /** Histórico horario por estación; las que están en mantenimiento o fuera de línea dejan de reportar antes. */
    private void cargarHistorico() {
        LocalDateTime fin = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime inicio = fin.minusDays(diasHistorico);
        long total = 0;
        for (Estacion estacion : estaciones.findAll()) {
            LocalDateTime finEstacion = switch (estacion.getEstado()) {
                case MANTENIMIENTO -> fin.minusHours(30);
                case FUERA_DE_LINEA -> fin.minusDays(9);
                case EN_LINEA -> fin;
            };
            Map<LocalDateTime, Fila> filas = new LinkedHashMap<>();
            int indice = 0;
            for (LocalDateTime t = inicio; !t.isAfter(finEstacion); t = t.plusHours(1), indice++) {
                GeneradorCalidadAireSintetico.Lectura l = generador.siguiente(estacion.getNombre(), t);
                Fila f = new Fila(t);
                f.pm25 = l.pm25();
                f.pm10 = l.pm10();
                f.temperatura = l.temperatura();
                f.humedad = l.humedad();
                // Errores de sensor reproducibles para que la validación y el histórico los muestren.
                if (indice % 997 == 0) { f.humedad = null; f.estado = EstadoMedicion.ADVERTENCIA; }
                if (indice % 1499 == 0) { f.pm25 = 612.0; f.estado = EstadoMedicion.DESCARTADA; }
                filas.put(t, f);
            }
            inyectarEpisodios(estacion, filas, fin);

            List<Medicion> lote = new ArrayList<>(TAMANO_BLOQUE);
            for (Fila f : filas.values()) {
                lote.add(new Medicion(estacion.getId(), f.fecha, f.pm25, f.pm10, f.temperatura, f.humedad,
                        estacion.getTipoFuente(), f.estado, null));
                if (lote.size() == TAMANO_BLOQUE) {
                    insercion.insertar(lote, Medicion.class);
                    lote = new ArrayList<>(TAMANO_BLOQUE);
                }
            }
            insercion.insertar(lote, Medicion.class);
            total += filas.size();
        }
        log.info("Histórico sintético generado: {} mediciones horarias", total);
    }

    /** Episodios de contaminación reproducibles que los módulos de anomalías y alertas deben detectar. */
    private static void inyectarEpisodios(Estacion estacion, Map<LocalDateTime, Fila> filas, LocalDateTime fin) {
        switch (estacion.getNombre()) {
            case "Estación Centro" -> {
                pico(filas, fin.minusHours(5), 148.6);
                pico(filas, fin.minusHours(6), 71.2);
            }
            case "Estación Parque Norte" -> pico(filas, fin.minusDays(2).minusHours(4), 92.4);
            case "Estación Poblado Sur" -> pico(filas, fin.minusDays(4).minusHours(9), 58.3);
            default -> {
            }
        }
    }

    private static void pico(Map<LocalDateTime, Fila> filas, LocalDateTime fecha, double pm25) {
        Fila f = filas.get(fecha);
        if (f == null) return;
        f.pm25 = pm25;
        f.pm10 = Math.round(pm25 * 1.7 * 10) / 10.0;
        f.estado = EstadoMedicion.VALIDA;
    }

    private void detectarEventosRecientes() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Medicion> ultimoDia = new ArrayList<>();
        for (Estacion estacion : estaciones.findAll()) {
            anomaliaService.analizar(estacion.getId(), ahora.minusDays(7), ahora);
            ultimoDia.addAll(mediciones.buscarPorEstacionYRango(estacion.getId(), ahora.minusHours(24), ahora));
        }
        alertaService.evaluarMediciones(ultimoDia);
    }

    /** Entrena una versión por algoritmo y pone en producción la más precisa (mayor R²). */
    private void entrenarModelosIniciales() {
        List<VersionModelo> entrenadas = new ArrayList<>();
        for (AlgoritmoModelo algoritmo : AlgoritmoModelo.values()) {
            entrenadas.add(entrenamientoService.entrenar(algoritmo).modelo());
        }
        entrenadas.stream().max(Comparator.comparingDouble(VersionModelo::getR2))
                .ifPresent(mejor -> entrenamientoService.desplegar(mejor.getId()));
    }
}
