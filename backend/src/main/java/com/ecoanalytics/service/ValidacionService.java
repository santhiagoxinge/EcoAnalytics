package com.ecoanalytics.service;

import com.ecoanalytics.model.EstadoMedicion;
import com.ecoanalytics.model.validacion.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Aplica todas las reglas de validación a un lote. Ningún registro se da por correcto solo porque llegó. */
@Service
public class ValidacionService {

    private final List<ReglaValidacion> reglas = List.of(
            new ReglaIdentificacion(), new ReglaValoresMedicion(), new ReglaDuplicado());

    public record ResultadoRegistro(RegistroMedicionCrudo registro, EstadoMedicion estado, List<ViolacionRegla> violaciones) {
    }

    public List<ResultadoRegistro> validar(List<RegistroMedicionCrudo> registros, Set<String> nombresEstaciones) {
        Set<String> conocidas = new HashSet<>();
        nombresEstaciones.forEach(n -> conocidas.add(n.trim().toLowerCase()));
        ReglaValidacion.Contexto contexto = new ReglaValidacion.Contexto(conocidas, new HashSet<>());

        List<ResultadoRegistro> resultados = new ArrayList<>(registros.size());
        for (RegistroMedicionCrudo registro : registros) {
            List<ViolacionRegla> violaciones = new ArrayList<>();
            for (ReglaValidacion regla : reglas) {
                if (regla.aplicaDespuesDe(violaciones)) violaciones.addAll(regla.verificar(registro, contexto));
            }
            resultados.add(new ResultadoRegistro(registro, estadoSegun(violaciones), violaciones));
        }
        return resultados;
    }

    private static EstadoMedicion estadoSegun(List<ViolacionRegla> violaciones) {
        if (violaciones.isEmpty()) return EstadoMedicion.VALIDA;
        return violaciones.stream().anyMatch(ViolacionRegla::descartaRegistro)
                ? EstadoMedicion.DESCARTADA
                : EstadoMedicion.ADVERTENCIA;
    }
}
