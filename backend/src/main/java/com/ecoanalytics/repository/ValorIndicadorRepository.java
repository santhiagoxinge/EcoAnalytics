package com.ecoanalytics.repository;

import com.ecoanalytics.model.TipoIndicador;
import com.ecoanalytics.model.ValorIndicador;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ValorIndicadorRepository extends MongoRepository<ValorIndicador, Long> {

    Optional<ValorIndicador> findFirstByEstacionIdAndTipoOrderByFechaCalculoDesc(Long estacionId, TipoIndicador tipo);
}
