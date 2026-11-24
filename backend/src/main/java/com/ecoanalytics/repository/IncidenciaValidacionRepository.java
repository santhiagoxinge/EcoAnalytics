package com.ecoanalytics.repository;

import com.ecoanalytics.model.IncidenciaValidacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IncidenciaValidacionRepository extends MongoRepository<IncidenciaValidacion, Long> {

    List<IncidenciaValidacion> findByLoteIdOrderByFilaAsc(Long loteId);
}
