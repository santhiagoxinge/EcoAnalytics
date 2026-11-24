package com.ecoanalytics.service;

import com.ecoanalytics.infraestructura.seguridad.ActorActual;
import com.ecoanalytics.model.EventoAuditoria;
import com.ecoanalytics.repository.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Bitácora de operaciones del sistema (consola técnica, HU-16). */
@Service
public class AuditoriaService {

    private final EventoAuditoriaRepository repositorio;

    public AuditoriaService(EventoAuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void info(String modulo, String descripcion) {
        registrar(EventoAuditoria.Nivel.INFO, modulo, descripcion, ActorActual.nombre());
    }

    /** Para operaciones previas a la autenticación, como iniciar sesión o registrarse. */
    public void infoComo(String actor, String modulo, String descripcion) {
        registrar(EventoAuditoria.Nivel.INFO, modulo, descripcion, actor);
    }

    public void advertencia(String modulo, String descripcion) {
        registrar(EventoAuditoria.Nivel.ADVERTENCIA, modulo, descripcion, ActorActual.nombre());
    }

    public void error(String modulo, String descripcion) {
        registrar(EventoAuditoria.Nivel.ERROR, modulo, descripcion, ActorActual.nombre());
    }

    public List<EventoAuditoria> recientes(EventoAuditoria.Nivel nivel) {
        return nivel == null
                ? repositorio.findTop200ByOrderByFechaDesc()
                : repositorio.findTop200ByNivelOrderByFechaDesc(nivel);
    }

    private void registrar(EventoAuditoria.Nivel nivel, String modulo, String descripcion, String actor) {
        repositorio.save(new EventoAuditoria(nivel, modulo, descripcion, actor));
    }
}
