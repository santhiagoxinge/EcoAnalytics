package com.ecoanalytics.dto;

import com.ecoanalytics.model.Alerta;
import com.ecoanalytics.model.EstadoEstacion;
import com.ecoanalytics.model.Rol;
import com.ecoanalytics.model.TipoFuente;
import com.ecoanalytics.model.prediccion.AlgoritmoModelo;
import com.ecoanalytics.model.reporte.DocumentoReporte;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Cuerpos de las peticiones con sus reglas de validación; los mensajes se muestran al usuario. */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record InicioSesion(
            @NotBlank(message = "El campo de correo electrónico es obligatorio.")
            @Email(message = "Ingrese un formato de correo electrónico válido.") String correo,
            @NotBlank(message = "La contraseña es obligatoria.") String contrasena) {
    }

    public record Registro(
            @NotBlank(message = "El nombre es obligatorio.") @Size(max = 120) String nombreCompleto,
            @NotBlank(message = "El campo de correo electrónico es obligatorio.")
            @Email(message = "Ingrese un formato de correo electrónico válido.") String correo,
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, message = "La contraseña debe contener al menos 8 caracteres.") String contrasena,
            @NotNull(message = "Seleccione un perfil.") Rol rol) {
    }

    public record UsuarioSolicitud(
            @NotBlank(message = "El nombre y apellidos son obligatorios.") @Size(max = 120) String nombreCompleto,
            @NotBlank(message = "El correo electrónico es obligatorio.")
            @Email(message = "Ingrese un formato de correo electrónico válido.") String correo,
            @NotNull(message = "Seleccione un rol.") Rol rol,
            @Size(min = 8, message = "La contraseña debe contener al menos 8 caracteres.") String contrasena) {
    }

    public record EstacionSolicitud(
            @NotBlank(message = "El nombre de la estación es obligatorio.") @Size(max = 120) String nombre,
            @NotBlank(message = "La comuna o zona es obligatoria.") @Size(max = 120) String zona,
            @NotNull(message = "La latitud es obligatoria.") Double latitud,
            @NotNull(message = "La longitud es obligatoria.") Double longitud,
            @NotNull(message = "Seleccione el tipo de fuente.") TipoFuente tipoFuente,
            @NotBlank(message = "Seleccione las variables de medición.") String variables) {
    }

    public record CambioEstado(@NotNull(message = "Seleccione un estado.") EstadoEstacion estado) {
    }

    public record CambioHabilitado(boolean habilitado) {
    }

    public record CambioUmbral(@NotNull @DecimalMin("1.5") @DecimalMax("6.0") Double umbral) {
    }

    public record Entrenamiento(@NotNull(message = "Seleccione un algoritmo.") AlgoritmoModelo algoritmo) {
    }

    public record AlertaExtraordinaria(
            @NotBlank(message = "El título es obligatorio.") @Size(max = 160) String titulo,
            @NotBlank(message = "Redacte el comunicado.") @Size(max = 600) String mensaje,
            @NotNull(message = "Seleccione el nivel.") Alerta.Nivel nivel,
            Long estacionId) {
    }

    public record ReporteSolicitud(
            @NotNull(message = "Seleccione el tipo de reporte.") DocumentoReporte.TipoReporte tipo,
            @NotNull(message = "Seleccione una estación.") Long estacionId,
            @NotNull(message = "Indique la fecha inicial.") LocalDate desde,
            @NotNull(message = "Indique la fecha final.") LocalDate hasta) {
    }
}
