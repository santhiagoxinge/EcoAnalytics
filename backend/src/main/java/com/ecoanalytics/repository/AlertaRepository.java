package com.ecoanalytics.repository;

import com.ecoanalytics.model.Alerta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertaRepository extends MongoRepository<Alerta, Long> {

    List<Alerta> findTop100ByOrderByFechaCreacionDesc();

    List<Alerta> findByEstadoOrderByFechaCreacionDesc(Alerta.Estado estado);

    boolean existsByEstacionIdAndTipoAndFechaCreacionAfter(Long estacionId, Alerta.Tipo tipo, LocalDateTime desde);

    long countByFechaCreacionAfter(LocalDateTime desde);
}
