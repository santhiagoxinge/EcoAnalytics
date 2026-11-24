package com.ecoanalytics.controller;

import com.ecoanalytics.dto.Respuestas.UsuarioRespuesta;
import com.ecoanalytics.dto.Solicitudes.UsuarioSolicitud;
import com.ecoanalytics.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioRespuesta> buscar(@RequestParam(required = false) String q) {
        return usuarioService.buscar(q).stream().map(UsuarioRespuesta::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta crear(@Valid @RequestBody UsuarioSolicitud s) {
        return UsuarioRespuesta.de(usuarioService.crear(s.nombreCompleto(), s.correo(), s.rol(), s.contrasena()));
    }

    @PutMapping("/{id}")
    public UsuarioRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioSolicitud s) {
        return UsuarioRespuesta.de(usuarioService.actualizar(id, s.nombreCompleto(), s.correo(), s.rol()));
    }

    @PostMapping("/{id}/desactivar")
    public UsuarioRespuesta desactivar(@PathVariable Long id) {
        return UsuarioRespuesta.de(usuarioService.desactivar(id));
    }

    @PostMapping("/{id}/activar")
    public UsuarioRespuesta activar(@PathVariable Long id) {
        return UsuarioRespuesta.de(usuarioService.activar(id));
    }
}
