package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.infraestructura.ingesta.FabricaFuentesMediciones;
import com.ecoanalytics.infraestructura.seguridad.ActorActual;
import com.ecoanalytics.model.*;
import com.ecoanalytics.model.evento.MedicionesRegistradasEvento;
import com.ecoanalytics.model.ingesta.AdaptadorFuenteMediciones;
import com.ecoanalytics.model.ingesta.FuenteInvalidaException;
import com.ecoanalytics.model.validacion.RegistroMedicionCrudo;
import com.ecoanalytics.model.validacion.ViolacionRegla;
import com.ecoanalytics.repository.*;
import com.ecoanalytics.service.ValidacionService.ResultadoRegistro;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Flujo Extraer → Validar → (revisión) → Transformar → Cargar. Los registros leídos esperan en memoria
 * hasta que el usuario confirma el lote, así nada inválido se guarda por accidente (HU-04, HU-05).
 */
@Service
public class IngestaService {

    private static final int MAX_INCIDENCIAS_DEVUELTAS = 300;

    private final FabricaFuentesMediciones fabricaFuentes;
    private final ValidacionService validacion;
    private final EstacionRepository estaciones;
    private final MedicionRepository mediciones;
    private final LoteIngestaRepository lotes;
    private final IncidenciaValidacionRepository incidencias;
    private final InsercionMasiva insercion;
    private final ApplicationEventPublisher eventos;
    private final AuditoriaService auditoria;
    private final Map<Long, List<ResultadoRegistro>> lotesPendientes = new java.util.concurrent.ConcurrentHashMap<>();

    public IngestaService(FabricaFuentesMediciones fabricaFuentes, ValidacionService validacion,
                          EstacionRepository estaciones, MedicionRepository mediciones, LoteIngestaRepository lotes,
                          IncidenciaValidacionRepository incidencias, InsercionMasiva insercion,
                          ApplicationEventPublisher eventos, AuditoriaService auditoria) {
        this.fabricaFuentes = fabricaFuentes;
        this.validacion = validacion;
        this.estaciones = estaciones;
        this.mediciones = mediciones;
        this.lotes = lotes;
        this.incidencias = incidencias;
        this.insercion = insercion;
        this.eventos = eventos;
        this.auditoria = auditoria;
    }

    public record ResultadoCarga(LoteIngesta lote, List<String> columnas, List<RegistroMedicionCrudo> vistaPrevia,
                                 List<IncidenciaValidacion> incidencias) {
    }

    public record ResultadoImportacion(LoteIngesta lote, int importados, int descartados, int interpolados,
                                       int yaExistentes) {
    }

    public ResultadoCarga cargar(MultipartFile archivo) {
        String nombreArchivo = Optional.ofNullable(archivo.getOriginalFilename()).orElse("archivo");
        AdaptadorFuenteMediciones adaptador;
        AdaptadorFuenteMediciones.FuenteLeida leida;
        try (InputStream entrada = archivo.getInputStream()) {
            adaptador = fabricaFuentes.paraArchivo(nombreArchivo);
            leida = adaptador.leer(entrada);
        } catch (FuenteInvalidaException e) {
            auditoria.advertencia("Ingesta", "Archivo rechazado (" + nombreArchivo + "): " + e.getMessage());
            throw ExcepcionNegocio.invalido(e.getMessage());
        } catch (IOException e) {
            throw ExcepcionNegocio.invalido("No fue posible leer el archivo cargado.");
        }
        if (leida.registros().isEmpty()) {
            throw ExcepcionNegocio.invalido("El archivo no contiene registros de mediciones.");
        }

        Set<String> nombresEstaciones = estaciones.findAll().stream().map(Estacion::getNombre).collect(Collectors.toSet());
        List<ResultadoRegistro> resultados = validacion.validar(leida.registros(), nombresEstaciones);

        LoteIngesta lote = new LoteIngesta(nombreArchivo, adaptador.tipo(), ActorActual.nombre());
        lote.resumir(resultados.size(),
                contar(resultados, EstadoMedicion.VALIDA),
                contar(resultados, EstadoMedicion.ADVERTENCIA),
                contar(resultados, EstadoMedicion.DESCARTADA));
        lote = lotes.save(lote);

        List<IncidenciaValidacion> incidenciasLote = new ArrayList<>();
        for (ResultadoRegistro resultado : resultados) {
            for (ViolacionRegla v : resultado.violaciones()) {
                incidenciasLote.add(new IncidenciaValidacion(lote.getId(), resultado.registro().fila(),
                        resultado.registro().estacionNombre(), v.variable(), v.valorDetectado(), v.rangoAceptable(),
                        v.tipo(), v.resolucion()));
            }
        }
        insercion.insertar(incidenciasLote, IncidenciaValidacion.class);
        lotesPendientes.put(lote.getId(), resultados);

        auditoria.info("Ingesta", "Archivo " + nombreArchivo + " analizado: " + resultados.size() + " registros, "
                + lote.getRegistrosRechazados() + " rechazados, " + lote.getRegistrosAdvertencia() + " con advertencia");
        return new ResultadoCarga(lote, leida.columnas(), leida.registros().stream().limit(5).toList(),
                incidenciasLote.stream().limit(MAX_INCIDENCIAS_DEVUELTAS).toList());
    }

    /**
     * Aplica la corrección automática y guarda el lote: los registros descartados se excluyen y los campos
     * faltantes se completan con la mediana de la estación dentro del lote.
     */
    public ResultadoImportacion confirmar(Long loteId) {
        LoteIngesta lote = buscarLote(loteId);
        lote.verificarPendiente();
        List<ResultadoRegistro> resultados = lotesPendientes.remove(loteId);
        if (resultados == null) {
            throw ExcepcionNegocio.conflicto("El lote expiró. Vuelva a cargar el archivo para importarlo.");
        }
        Map<String, Estacion> estacionesPorNombre = estaciones.findAll().stream()
                .collect(Collectors.toMap(e -> e.getNombre().toLowerCase(), Function.identity()));
        Map<String, double[]> medianas = medianasPorEstacion(resultados);

        List<Medicion> aGuardar = new ArrayList<>();
        int descartados = 0, interpolados = 0, yaExistentes = 0;
        for (ResultadoRegistro resultado : resultados) {
            if (resultado.estado() == EstadoMedicion.DESCARTADA) {
                descartados++;
                continue;
            }
            RegistroMedicionCrudo r = resultado.registro();
            String clave = r.estacionNombre().trim().toLowerCase();
            Estacion estacion = estacionesPorNombre.get(clave);
            if (mediciones.existsByEstacionIdAndFechaMedicion(estacion.getId(), r.fecha())) {
                yaExistentes++;
                continue;
            }
            double[] m = medianas.get(clave);
            RegistroMedicionCrudo completo = r.conValores(r.pm25(),
                    completar(r.pm10(), m[0]), completar(r.temperatura(), m[1]), completar(r.humedad(), m[2]));
            if (!completo.equals(r)) interpolados++;
            aGuardar.add(new Medicion(estacion.getId(), r.fecha(), completo.pm25(), completo.pm10(),
                    completo.temperatura(), completo.humedad(), lote.getFormato(), resultado.estado(), lote.getId()));
        }
        List<Long> ids = insercion.insertar(aGuardar, Medicion.class).stream().map(Medicion::getId).toList();
        lote.marcarImportado(ids.size());
        lotes.save(lote);
        eventos.publishEvent(new MedicionesRegistradasEvento(ids));

        auditoria.info("Ingesta", "Ingesta exitosa del lote #" + lote.getId() + ": " + ids.size()
                + " registros incorporados, " + descartados + " excluidos, " + interpolados + " interpolados");
        return new ResultadoImportacion(lote, ids.size(), descartados, interpolados, yaExistentes);
    }

    public void cancelar(Long loteId) {
        LoteIngesta lote = buscarLote(loteId);
        lote.cancelar();
        lotesPendientes.remove(loteId);
        lotes.save(lote);
        auditoria.info("Ingesta", "Lote #" + loteId + " descartado sin importar");
    }

    public List<LoteIngesta> lotesRecientes() {
        return lotes.findTop20ByOrderByFechaCreacionDesc();
    }

    /** Reporte de calidad de un lote en CSV (Alta Cohesión: el formato del reporte vive con su servicio). */
    public String exportarIncidenciasCsv(Long loteId) {
        buscarLote(loteId);
        StringBuilder csv = new StringBuilder("ID,Fila,Estación,Variable,Valor detectado,Rango aceptable,Tipo de problema,Estado final\n");
        for (IncidenciaValidacion i : incidencias.findByLoteIdOrderByFilaAsc(loteId)) {
            csv.append(i.getId()).append(',').append(i.getFila()).append(',')
                    .append(celda(i.getEstacionNombre())).append(',').append(celda(i.getVariable())).append(',')
                    .append(celda(i.getValorDetectado())).append(',').append(celda(i.getRangoAceptable())).append(',')
                    .append(i.getTipo()).append(',').append(i.getResolucion()).append('\n');
        }
        return csv.toString();
    }

    private LoteIngesta buscarLote(Long id) {
        return lotes.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("Lote no encontrado."));
    }

    private static int contar(List<ResultadoRegistro> resultados, EstadoMedicion estado) {
        return (int) resultados.stream().filter(r -> r.estado() == estado).count();
    }

    private static Double completar(Double valor, double mediana) {
        return valor != null || Double.isNaN(mediana) ? valor : mediana;
    }

    private static String celda(String valor) {
        return valor == null ? "" : valor.replace(',', ' ');
    }

    /** Mediana de PM10, temperatura y humedad por estación, sobre los registros utilizables del lote. */
    private static Map<String, double[]> medianasPorEstacion(List<ResultadoRegistro> resultados) {
        Map<String, List<RegistroMedicionCrudo>> porEstacion = resultados.stream()
                .filter(r -> r.estado() != EstadoMedicion.DESCARTADA)
                .map(ResultadoRegistro::registro)
                .collect(Collectors.groupingBy(r -> r.estacionNombre().trim().toLowerCase()));
        Map<String, double[]> medianas = new HashMap<>();
        porEstacion.forEach((estacion, registros) -> medianas.put(estacion, new double[]{
                mediana(registros.stream().map(RegistroMedicionCrudo::pm10)),
                mediana(registros.stream().map(RegistroMedicionCrudo::temperatura)),
                mediana(registros.stream().map(RegistroMedicionCrudo::humedad))}));
        return medianas;
    }

    private static double mediana(Stream<Double> valores) {
        double[] ordenados = valores.filter(Objects::nonNull).mapToDouble(Double::doubleValue).sorted().toArray();
        if (ordenados.length == 0) return Double.NaN;
        int medio = ordenados.length / 2;
        double mediana = ordenados.length % 2 == 0 ? (ordenados[medio - 1] + ordenados[medio]) / 2 : ordenados[medio];
        return Estadistica.redondear(mediana);
    }
}
