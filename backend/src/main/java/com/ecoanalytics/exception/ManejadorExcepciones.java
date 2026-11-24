package com.ecoanalytics.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones a respuestas HTTP con un cuerpo uniforme: { mensaje, campos }. */
@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    public record ErrorApi(String mensaje, Map<String, String> campos) {
    }

    @ExceptionHandler(ExcepcionNegocio.class)
    ResponseEntity<ErrorApi> negocio(ExcepcionNegocio e) {
        return ResponseEntity.status(estadoHttp(e.tipo())).body(new ErrorApi(e.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String primero = campos.values().stream().findFirst().orElse("Revise los datos ingresados.");
        return ResponseEntity.badRequest().body(new ErrorApi(primero, campos));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorApi> tipoIncorrecto(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErrorApi("Parámetro no válido: " + e.getName(), Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorApi> prohibido(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorApi("Su rol no tiene permisos para realizar esta acción.", Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorApi> archivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorApi("El archivo supera el tamaño máximo permitido de 20 MB.", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorApi("Ocurrió un error inesperado. Intente nuevamente en unos minutos.", Map.of()));
    }

    private static HttpStatus estadoHttp(ExcepcionNegocio.Tipo tipo) {
        return switch (tipo) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case INVALIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CONFLICTO -> HttpStatus.CONFLICT;
            case NO_AUTORIZADO -> HttpStatus.UNAUTHORIZED;
            case PROHIBIDO -> HttpStatus.FORBIDDEN;
        };
    }
}
