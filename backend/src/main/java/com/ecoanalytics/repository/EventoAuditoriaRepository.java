package com.ecoanalytics.repository;

import com.ecoanalytics.model.EventoAuditoria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventoAuditoriaRepository extends MongoRepository<EventoAuditoria, Long> {

    List<EventoAuditoria> findTop200ByOrderByFechaDesc();

    List<EventoAuditoria> findTop200ByNivelOrderByFechaDesc(EventoAuditoria.Nivel nivel);
}
