package com.ecoanalytics.model.validacion;

import com.ecoanalytics.model.IncidenciaValidacion.Resolucion;
import com.ecoanalytics.model.IncidenciaValidacion.Tipo;
import com.ecoanalytics.model.VariableAmbiental;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Verifica presencia y rango físico de cada variable. Si falta PM2.5 el registro se descarta
 * (es la variable principal); los demás faltantes se marcan y luego pueden interpolarse.
 */
public class ReglaValoresMedicion implements ReglaValidacion {

    @Override
    public List<ViolacionRegla> verificar(RegistroMedicionCrudo registro, Contexto contexto) {
        List<ViolacionRegla> violaciones = new ArrayList<>();
        verificarVariable(VariableAmbiental.PM25, registro.pm25(), violaciones);
        verificarVariable(VariableAmbiental.PM10, registro.pm10(), violaciones);
        verificarVariable(VariableAmbiental.TEMPERATURA, registro.temperatura(), violaciones);
        verificarVariable(VariableAmbiental.HUMEDAD, registro.humedad(), violaciones);
        return violaciones;
    }

    private void verificarVariable(VariableAmbiental variable, Double valor, List<ViolacionRegla> violaciones) {
        if (valor == null) {
            Resolucion resolucion = variable == VariableAmbiental.PM25 ? Resolucion.DESCARTADO : Resolucion.MARCADO;
            violaciones.add(new ViolacionRegla(Tipo.CAMPO_FALTANTE, resolucion, variable.etiqueta(), "—",
                    variable.descripcionRango()));
        } else if (!variable.estaEnRango(valor)) {
            violaciones.add(new ViolacionRegla(Tipo.FUERA_DE_RANGO, Resolucion.DESCARTADO, variable.etiqueta(),
                    String.format(Locale.ROOT, "%.1f %s", valor, variable.unidad()), variable.descripcionRango()));
        }
    }
}
