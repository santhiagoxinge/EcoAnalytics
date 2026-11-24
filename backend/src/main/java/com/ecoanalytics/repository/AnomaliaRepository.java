package com.ecoanalytics.repository;

import com.ecoanalytics.model.Anomalia;
import com.ecoanalytics.model.VariableAmbiental;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnomaliaRepository extends MongoRepository<Anomalia, Long> {

    List<Anomalia> findTop100ByOrderByFechaMedicionDesc();

    List<Anomalia> findByEstacionIdAndFechaMedicionAfterOrderByFechaMedicionAsc(Long estacionId, LocalDateTime desde);

    long countByEstado(Anomalia.Estado estado);

    boolean existsByEstacionIdAndFechaMedicionAndVariable(Long estacionId, LocalDateTime fechaMedicion, VariableAmbiental variable);
}
