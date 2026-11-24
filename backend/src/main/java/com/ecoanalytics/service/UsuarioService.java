package com.ecoanalytics.service;

import com.ecoanalytics.exception.ExcepcionNegocio;
import com.ecoanalytics.infraestructura.seguridad.ActorActual;
import com.ecoanalytics.model.EstadoUsuario;
import com.ecoanalytics.model.Rol;
import com.ecoanalytics.model.Usuario;
import com.ecoanalytics.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/** Administración de cuentas (HU-02). */
@Service
public class UsuarioService {

    static final String CONTRASENA_POR_DEFECTO = "EcoAnalytics2026!";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder cifrador, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.auditoria = auditoria;
    }

    public List<Usuario> buscar(String termino) {
        if (termino == null || termino.isBlank()) return usuarios.findAllByOrderByNombreCompletoAsc();
        // Pattern.quote evita que el texto del usuario se interprete como expresión regular.
        return usuarios.buscarPorNombreOCorreo(Pattern.quote(termino.trim()));
    }

    public Usuario crear(String nombreCompleto, String correo, Rol rol, String contrasena) {
        String correoNormalizado = correo.trim().toLowerCase();
        if (usuarios.existsByCorreo(correoNormalizado)) {
            throw ExcepcionNegocio.conflicto("Ya existe un usuario con el correo " + correo + ".");
        }
        String clave = contrasena == null || contrasena.isBlank() ? CONTRASENA_POR_DEFECTO : contrasena;
        Usuario usuario = usuarios.save(new Usuario(nombreCompleto.trim(), correoNormalizado, cifrador.encode(clave),
                rol, EstadoUsuario.ACTIVO));
        auditoria.info("Usuarios", "Creación del usuario " + usuario.getCorreo() + " con rol " + rol);
        return usuario;
    }

    public Usuario actualizar(Long id, String nombreCompleto, String correo, Rol rol) {
        Usuario usuario = buscarPorId(id);
        String correoNormalizado = correo.trim().toLowerCase();
        usuarios.findByCorreo(correoNormalizado)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw ExcepcionNegocio.conflicto("El correo " + correo + " ya pertenece a otro usuario.");
                });
        if (esUsuarioActual(usuario) && rol != Rol.ADMINISTRADOR) {
            throw ExcepcionNegocio.invalido("No puede retirarse a sí mismo el rol de administrador.");
        }
        Rol anterior = usuario.getRol();
        usuario.actualizar(nombreCompleto.trim(), correoNormalizado, rol);
        usuarios.save(usuario);
        auditoria.info("Usuarios", "Actualización de " + usuario.getCorreo()
                + (anterior != rol ? " · rol " + anterior + " → " + rol : ""));
        return usuario;
    }

    public Usuario desactivar(Long id) {
        Usuario usuario = buscarPorId(id);
        if (esUsuarioActual(usuario)) {
            throw ExcepcionNegocio.invalido("No puede desactivar su propia cuenta.");
        }
        usuario.desactivar();
        usuarios.save(usuario);
        auditoria.advertencia("Usuarios", "Desactivación de la cuenta " + usuario.getCorreo());
        return usuario;
    }

    /** Reactiva una cuenta inactiva o aprueba a un analista pendiente. */
    public Usuario activar(Long id) {
        Usuario usuario = buscarPorId(id);
        boolean estabaPendiente = usuario.esPendiente();
        usuario.activar();
        usuarios.save(usuario);
        auditoria.info("Usuarios", (estabaPendiente ? "Aprobación" : "Reactivación") + " de la cuenta " + usuario.getCorreo());
        return usuario;
    }

    private Usuario buscarPorId(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("Usuario no encontrado."));
    }

    private static boolean esUsuarioActual(Usuario usuario) {
        return usuario.getCorreo().equalsIgnoreCase(ActorActual.nombre());
    }
}
