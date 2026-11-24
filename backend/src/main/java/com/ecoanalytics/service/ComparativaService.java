package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.model.*;
import com.ecoanalytics.repository.MedicionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Comparación de una variable entre dos periodos (HU-08). */
@Service
public class ComparativaService {

    /** Por debajo de esta fracción de lecturas horarias esperadas un periodo no se compara. */
    private static final double COBERTURA_MINIMA = 0.4;

    private final MedicionRepository mediciones;
    private final EstacionService estacionService;

    public ComparativaService(MedicionRepository mediciones, EstacionService estacionService) {
        this.mediciones = mediciones;
        this.estacionService = estacionService;
    }

    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    public record EstadisticaPeriodo(LocalDate desde, LocalDate hasta, int mediciones, double cobertura, Double media,
                                     Double maximo, Double minimo, Integer diasExcedencia, boolean suficiente) {
    }

    public record PuntoDia(int dia, Double periodoA, Double periodoB) {
    }

    public record Variaciones(Double cambioMedia, Double cambioMaximo, Double cambioMinimo, Integer cambioDiasExcedencia) {
    }

    public record Comparativa(String estacionNombre, VariableAmbiental variable, String unidad,
                              EstadisticaPeriodo periodoA, EstadisticaPeriodo periodoB, Variaciones variaciones,
                              boolean comparable, String advertencia, List<PuntoDia> serie) {
    }

    public Comparativa comparar(Long estacionId, VariableAmbiental variable, Periodo a, Periodo b) {
        if (a.hasta().isBefore(a.desde()) || b.hasta().isBefore(b.desde())) {
            throw ExcepcionNegocio.invalido("La fecha final de cada periodo debe ser posterior a la inicial.");
        }
        Estacion estacion = estacionService.buscar(estacionId);
        Map<LocalDate, List<Double>> diarioA = diario(estacionId, variable, a);
        Map<LocalDate, List<Double>> diarioB = diario(estacionId, variable, b);
        EstadisticaPeriodo estA = estadistica(a, variable, diarioA);
        EstadisticaPeriodo estB = estadistica(b, variable, diarioB);

        boolean comparable = estA.suficiente() && estB.suficiente();
        String advertencia = comparable ? null
                : "Datos insuficientes en el Periodo " + (!estA.suficiente() ? "A" : "B")
                + " seleccionado para generar una comparativa fiable";
        Variaciones variaciones = comparable
                ? new Variaciones(Estadistica.variacionPorcentual(estA.media(), estB.media()),
                Estadistica.variacionPorcentual(estA.maximo(), estB.maximo()),
                Estadistica.variacionPorcentual(estA.minimo(), estB.minimo()),
                estA.diasExcedencia() == null ? null : estB.diasExcedencia() - estA.diasExcedencia())
                : null;

        List<Double> mediasA = mediasDiarias(a, diarioA);
        List<Double> mediasB = mediasDiarias(b, diarioB);
        List<PuntoDia> serie = new ArrayList<>();
        for (int i = 0; i < Math.max(mediasA.size(), mediasB.size()); i++) {
            serie.add(new PuntoDia(i + 1, i < mediasA.size() ? mediasA.get(i) : null, i < mediasB.size() ? mediasB.get(i) : null));
        }
        return new Comparativa(estacion.getNombre(), variable, variable.unidad(), estA, estB, variaciones, comparable,
                advertencia, serie);
    }

    public String exportarCsv(Long estacionId, VariableAmbiental variable, Periodo a, Periodo b) {
        Comparativa c = comparar(estacionId, variable, a, b);
        if (!c.comparable()) throw ExcepcionNegocio.invalido(c.advertencia());
        return "Informe comparativo EcoAnalytics\n"
                + "Estación," + c.estacionNombre() + "\nVariable," + variable.etiqueta() + " (" + c.unidad() + ")\n\n"
                + "Métrica,Periodo A (" + a.desde() + " a " + a.hasta() + "),Periodo B (" + b.desde() + " a " + b.hasta() + "),Variación\n"
                + "Promedio," + c.periodoA().media() + "," + c.periodoB().media() + "," + c.variaciones().cambioMedia() + "%\n"
                + "Máximo," + c.periodoA().maximo() + "," + c.periodoB().maximo() + "," + c.variaciones().cambioMaximo() + "%\n"
                + "Mínimo," + c.periodoA().minimo() + "," + c.periodoB().minimo() + "," + c.variaciones().cambioMinimo() + "%\n"
                + "Días de excedencia," + texto(c.periodoA().diasExcedencia()) + "," + texto(c.periodoB().diasExcedencia())
                + "," + texto(c.variaciones().cambioDiasExcedencia()) + "\n"
                + "Mediciones," + c.periodoA().mediciones() + "," + c.periodoB().mediciones() + ",\n";
    }

    private Map<LocalDate, List<Double>> diario(Long estacionId, VariableAmbiental variable, Periodo p) {
        return mediciones.buscarUtilesPorEstacionYRango(estacionId,
                        p.desde().atStartOfDay(), p.hasta().plusDays(1).atStartOfDay().minusSeconds(1))
                .stream()
                .filter(m -> m.valor(variable) != null)
                .collect(Collectors.groupingBy(m -> m.getFechaMedicion().toLocalDate(), TreeMap::new,
                        Collectors.mapping(m -> m.valor(variable), Collectors.toList())));
    }

    private static EstadisticaPeriodo estadistica(Periodo p, VariableAmbiental variable, Map<LocalDate, List<Double>> diario) {
        List<Double> todos = diario.values().stream().flatMap(List::stream).toList();
        long horasEsperadas = (ChronoUnit.DAYS.between(p.desde(), p.hasta()) + 1) * 24;
        double cobertura = Estadistica.redondear(todos.size() / (double) horasEsperadas, 2);
        Double limite = switch (variable) {
            case PM25 -> IndiceCalidadAire.LIMITE_DIARIO_OMS_PM25;
            case PM10 -> IndiceCalidadAire.LIMITE_DIARIO_OMS_PM10;
            default -> null;
        };
        Integer excedencia = limite == null ? null : (int) diario.values().stream()
                .filter(valores -> Estadistica.media(valores) > limite).count();
        return new EstadisticaPeriodo(p.desde(), p.hasta(), todos.size(), cobertura, Estadistica.media(todos),
                Estadistica.maximo(todos), Estadistica.minimo(todos), excedencia, cobertura >= COBERTURA_MINIMA);
    }

    private static List<Double> mediasDiarias(Periodo p, Map<LocalDate, List<Double>> diario) {
        List<Double> medias = new ArrayList<>();
        for (LocalDate d = p.desde(); !d.isAfter(p.hasta()); d = d.plusDays(1)) {
            List<Double> valores = diario.get(d);
            medias.add(valores == null ? null : Estadistica.media(valores));
        }
        return medias;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
