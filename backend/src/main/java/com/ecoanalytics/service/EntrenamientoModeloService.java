package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.infraestructura.ml.FabricaModelosPredictivos;
import com.ecoanalytics.infraestructura.ml.RegistroModelos;
import com.ecoanalytics.infraestructura.seguridad.ActorActual;
import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.model.Estadistica;
import com.ecoanalytics.model.Medicion;
import com.ecoanalytics.model.VersionModelo;
import com.ecoanalytics.model.prediccion.*;
import com.ecoanalytics.model.prediccion.ConstructorCaracteristicas.PuntoHorario;
import com.ecoanalytics.repository.EstacionRepository;
import com.ecoanalytics.repository.MedicionRepository;
import com.ecoanalytics.repository.VersionModeloRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flujo de entrenamiento (caso de estudio, sección 15): histórico válido → dataset → entrenar → evaluar →
 * registrar (HU-12). Las lecturas descartadas nunca llegan al dataset (regla de negocio 4).
 */
@Service
public class EntrenamientoModeloService {

    public static final String VARIABLE_OBJETIVO = "PM2.5 a 24 horas";
    private static final int VENTANA_ENTRENAMIENTO_DIAS = 120;
    private static final int MIN_MUESTRAS = 500;
    private static final double PROPORCION_ENTRENAMIENTO = 0.8;
    private static final int PUNTOS_COMPARACION = 96;

    private final MedicionRepository mediciones;
    private final EstacionRepository estaciones;
    private final VersionModeloRepository versiones;
    private final FabricaModelosPredictivos fabrica;
    private final RegistroModelos registro;
    private final AuditoriaService auditoria;

    public EntrenamientoModeloService(MedicionRepository mediciones, EstacionRepository estaciones,
                                      VersionModeloRepository versiones, FabricaModelosPredictivos fabrica,
                                      RegistroModelos registro, AuditoriaService auditoria) {
        this.mediciones = mediciones;
        this.estaciones = estaciones;
        this.versiones = versiones;
        this.fabrica = fabrica;
        this.registro = registro;
        this.auditoria = auditoria;
    }

    public record PuntoComparacion(LocalDateTime fecha, double real, double predicho) {
    }

    public record ResultadoEntrenamiento(VersionModelo modelo, String estacionNombre, List<PuntoComparacion> comparacion) {
    }

    private record Dataset(List<MuestraEntrenamiento> entrenamiento, List<MuestraEntrenamiento> prueba,
                           List<PuntoHorario> puntosPruebaReferencia, List<MuestraEntrenamiento> muestrasPruebaReferencia,
                           String estacionReferencia) {
    }

    public ResultadoEntrenamiento entrenar(AlgoritmoModelo algoritmo) {
        Dataset dataset = construirDataset();
        long inicio = System.currentTimeMillis();
        ModeloPredictivo modelo = fabrica.crear(algoritmo);
        modelo.entrenar(dataset.entrenamiento());
        MetricasModelo metricas = modelo.evaluar(dataset.prueba(), dataset.entrenamiento().size());
        long duracion = System.currentTimeMillis() - inicio;

        String version = "v1." + versiones.count();
        VersionModelo nueva = new VersionModelo(version, algoritmo, VARIABLE_OBJETIVO,
                ConstructorCaracteristicas.DESCRIPCION, metricas, duracion, ActorActual.nombre());
        // El primer modelo entrenado entra directamente a producción para que haya pronósticos desde el inicio.
        if (versiones.findFirstByEstado(VersionModelo.Estado.PRODUCCION).isEmpty()) nueva.promover();
        nueva = versiones.save(nueva);
        registro.registrar(version, modelo);
        auditoria.info("Modelos", "Entrenamiento de " + version + " (" + algoritmo.nombre() + "): R² " + metricas.r2()
                + ", RMSE " + metricas.rmse() + ", MAE " + metricas.mae());

        List<PuntoComparacion> comparacion = new ArrayList<>();
        int desde = Math.max(0, dataset.muestrasPruebaReferencia().size() - PUNTOS_COMPARACION);
        for (int i = desde; i < dataset.muestrasPruebaReferencia().size(); i++) {
            MuestraEntrenamiento muestra = dataset.muestrasPruebaReferencia().get(i);
            comparacion.add(new PuntoComparacion(dataset.puntosPruebaReferencia().get(i).fecha(), muestra.objetivo(),
                    Estadistica.redondear(modelo.predecir(muestra.caracteristicas()))));
        }
        return new ResultadoEntrenamiento(nueva, dataset.estacionReferencia(), comparacion);
    }

    public List<VersionModelo> versiones() {
        return versiones.findAllByOrderByFechaEntrenamientoDesc();
    }

    /** Pone una versión en producción y archiva la anterior. */
    public VersionModelo desplegar(Long id) {
        VersionModelo objetivo = versiones.findById(id)
                .orElseThrow(() -> ExcepcionNegocio.noEncontrado("Modelo no encontrado."));
        if (objetivo.estaEnProduccion()) return objetivo;
        versiones.findFirstByEstado(VersionModelo.Estado.PRODUCCION).ifPresent(anterior -> {
            anterior.archivar();
            versiones.save(anterior);
        });
        objetivo.promover();
        versiones.save(objetivo);
        asegurarAjustado(objetivo);
        auditoria.advertencia("Modelos", "Despliegue en producción de " + objetivo.getVersion()
                + " (" + objetivo.getAlgoritmo().nombre() + ")");
        return objetivo;
    }

    public VersionModelo enProduccion() {
        return versiones.findFirstByEstado(VersionModelo.Estado.PRODUCCION)
                .orElseThrow(() -> ExcepcionNegocio.invalido("Predicción no disponible: no hay un modelo predictivo en producción."));
    }

    /** Los parámetros ajustados viven en memoria; tras un reinicio la versión se reentrena con su algoritmo. */
    public ModeloPredictivo asegurarAjustado(VersionModelo version) {
        return registro.buscar(version.getVersion()).orElseGet(() -> {
            ModeloPredictivo modelo = fabrica.crear(version.getAlgoritmo());
            modelo.entrenar(construirDataset().entrenamiento());
            registro.registrar(version.getVersion(), modelo);
            return modelo;
        });
    }

    /** Serie horaria de PM2.5 útil de una estación; temperatura y humedad faltantes arrastran el último valor. */
    public List<PuntoHorario> serieHoraria(Long estacionId, LocalDateTime desde, LocalDateTime hasta) {
        List<PuntoHorario> puntos = new ArrayList<>();
        double ultimaTemperatura = 18, ultimaHumedad = 70;
        for (Medicion m : mediciones.buscarUtilesPorEstacionYRango(estacionId, desde, hasta)) {
            if (m.getPm25() == null) continue;
            if (m.getTemperatura() != null) ultimaTemperatura = m.getTemperatura();
            if (m.getHumedad() != null) ultimaHumedad = m.getHumedad();
            puntos.add(new PuntoHorario(m.getFechaMedicion().withMinute(0).withSecond(0).withNano(0), m.getPm25(),
                    ultimaTemperatura, ultimaHumedad));
        }
        return puntos;
    }

    private Dataset construirDataset() {
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusDays(VENTANA_ENTRENAMIENTO_DIAS);
        List<MuestraEntrenamiento> entrenamiento = new ArrayList<>();
        List<MuestraEntrenamiento> prueba = new ArrayList<>();
        List<PuntoHorario> puntosReferencia = List.of();
        List<MuestraEntrenamiento> muestrasReferencia = List.of();
        String estacionReferencia = null;

        for (Estacion estacion : estaciones.findAllByOrderByNombreAsc()) {
            List<PuntoHorario> serie = serieHoraria(estacion.getId(), desde, hasta);
            List<MuestraEntrenamiento> muestras = ConstructorCaracteristicas.muestras(serie);
            if (muestras.size() < 100) continue;
            int corte = (int) (muestras.size() * PROPORCION_ENTRENAMIENTO);
            entrenamiento.addAll(muestras.subList(0, corte));
            List<MuestraEntrenamiento> pruebaEstacion = muestras.subList(corte, muestras.size());
            prueba.addAll(pruebaEstacion);
            // La gráfica de comparación usa la estación con más datos de prueba.
            if (pruebaEstacion.size() > muestrasReferencia.size()) {
                estacionReferencia = estacion.getNombre();
                muestrasReferencia = pruebaEstacion;
                puntosReferencia = ConstructorCaracteristicas.puntosObjetivo(serie).subList(corte, muestras.size());
            }
        }
        if (entrenamiento.size() < MIN_MUESTRAS) {
            throw ExcepcionNegocio.invalido("Datos insuficientes para entrenar: se requieren al menos " + MIN_MUESTRAS
                    + " registros horarios válidos y contiguos.");
        }
        return new Dataset(entrenamiento, prueba, puntosReferencia, muestrasReferencia, estacionReferencia);
    }
}
