package com.ecoanalytics.model.validacion;

import com.ecoanalytics.model.IncidenciaValidacion.Resolucion;
import com.ecoanalytics.model.IncidenciaValidacion.Tipo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estación, fecha y hora identifican una medición; sin ellas el registro se descarta.
 * Una lectura no puede venir del futuro (una hora de tolerancia cubre desfases de reloj entre fuentes).
 */
public class ReglaIdentificacion implements ReglaValidacion {

    @Override
    public List<ViolacionRegla> verificar(RegistroMedicionCrudo registro, Contexto contexto) {
        List<ViolacionRegla> violaciones = new ArrayList<>();
        if (registro.estacionNombre() == null || registro.estacionNombre().isBlank()) {
            violaciones.add(new ViolacionRegla(Tipo.CAMPO_FALTANTE, Resolucion.DESCARTADO, "Estación", "—", "Obligatorio"));
        } else if (!contexto.estacionesConocidas().contains(registro.estacionNombre().trim().toLowerCase())) {
            violaciones.add(new ViolacionRegla(Tipo.ESTACION_DESCONOCIDA, Resolucion.DESCARTADO, "Estación",
                    registro.estacionNombre(), "Estación registrada"));
        }
        if (registro.fecha() != null && registro.fecha().isAfter(LocalDateTime.now().plusHours(1))) {
            violaciones.add(new ViolacionRegla(Tipo.FECHA_INVALIDA, Resolucion.DESCARTADO, "Fecha/Hora",
                    registro.fechaOriginal(), "No posterior a la fecha actual"));
        }
        if (registro.fecha() == null) {
            boolean vacia = registro.fechaOriginal() == null || registro.fechaOriginal().isBlank();
            violaciones.add(new ViolacionRegla(vacia ? Tipo.CAMPO_FALTANTE : Tipo.FECHA_INVALIDA, Resolucion.DESCARTADO,
                    "Fecha/Hora", vacia ? "—" : registro.fechaOriginal(), "yyyy-MM-dd HH:mm"));
        }
        return violaciones;
    }
}
