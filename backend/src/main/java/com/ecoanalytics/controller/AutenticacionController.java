package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.SesionRespuesta;
import com.ecoanalytics.dto.Respuestas.UsuarioRespuesta;
import com.ecoanalytics.dto.Solicitudes.InicioSesion;
import com.ecoanalytics.dto.Solicitudes.Registro;
import com.ecoanalytics.service.AutenticacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Inicio de sesión, registro público y perfil del usuario actual (HU-01). */
@RestController
@RequestMapping("/api")
public class AutenticacionController {

    private final AutenticacionService autenticacion;

    public AutenticacionController(AutenticacionService autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/auth/login")
    public SesionRespuesta iniciarSesion(@Valid @RequestBody InicioSesion solicitud) {
        AutenticacionService.Sesion sesion = autenticacion.iniciarSesion(solicitud.correo(), solicitud.contrasena());
        return new SesionRespuesta(sesion.token(), UsuarioRespuesta.de(sesion.usuario()));
    }

    @PostMapping("/auth/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta registrar(@Valid @RequestBody Registro solicitud) {
        return UsuarioRespuesta.de(autenticacion.registrar(solicitud.nombreCompleto(), solicitud.correo(),
                solicitud.contrasena(), solicitud.rol()));
    }

    @GetMapping("/perfil")
    public UsuarioRespuesta perfil(Authentication autenticado) {
        return UsuarioRespuesta.de(autenticacion.actual(autenticado.getName()));
    }
}
