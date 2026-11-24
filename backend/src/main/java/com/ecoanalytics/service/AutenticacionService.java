package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.infraestructura.seguridad.TokenService;
import com.ecoanalytics.model.EstadoUsuario;
import com.ecoanalytics.model.Rol;
import com.ecoanalytics.model.Usuario;
import com.ecoanalytics.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    /** El mismo mensaje exista o no el correo, para que no se puedan enumerar cuentas. */
    private static final String CREDENCIALES_INVALIDAS = "Credenciales incorrectas. Verifique su correo y contraseña.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final TokenService tokenService;
    private final AuditoriaService auditoria;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder cifrador, TokenService tokenService,
                                AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.tokenService = tokenService;
        this.auditoria = auditoria;
    }

    public record Sesion(String token, Usuario usuario) {
    }

    public Sesion iniciarSesion(String correo, String contrasena) {
        Usuario usuario = usuarios.findByCorreo(correo.trim().toLowerCase())
                .filter(u -> cifrador.matches(contrasena, u.getContrasenaCifrada()))
                .orElseThrow(() -> ExcepcionNegocio.noAutorizado(CREDENCIALES_INVALIDAS));
        usuario.validarInicioSesion();
        usuario.registrarIngreso();
        usuarios.save(usuario);
        auditoria.infoComo(usuario.getCorreo(), "Autenticación", "Inicio de sesión (" + usuario.getRol() + ")");
        return new Sesion(tokenService.emitir(usuario), usuario);
    }

    /** Registro público. Los ciudadanos quedan activos; los analistas esperan la verificación de un administrador. */
    public Usuario registrar(String nombreCompleto, String correo, String contrasena, Rol rolSolicitado) {
        if (rolSolicitado == Rol.ADMINISTRADOR) {
            throw ExcepcionNegocio.invalido("El rol de administrador solo puede asignarlo otro administrador.");
        }
        String correoNormalizado = correo.trim().toLowerCase();
        if (usuarios.existsByCorreo(correoNormalizado)) {
            throw ExcepcionNegocio.conflicto("Ya existe una cuenta registrada con ese correo electrónico.");
        }
        EstadoUsuario estado = rolSolicitado == Rol.ANALISTA ? EstadoUsuario.PENDIENTE : EstadoUsuario.ACTIVO;
        Usuario usuario = usuarios.save(new Usuario(nombreCompleto.trim(), correoNormalizado,
                cifrador.encode(contrasena), rolSolicitado, estado));
        auditoria.infoComo(usuario.getCorreo(), "Usuarios", "Registro público de " + usuario.getCorreo() + " como "
                + rolSolicitado + (usuario.esPendiente() ? " (pendiente de verificación)" : ""));
        return usuario;
    }

    public Usuario actual(String correo) {
        return usuarios.findByCorreo(correo.toLowerCase())
                .orElseThrow(() -> ExcepcionNegocio.noAutorizado("Sesión no válida."));
    }
}
