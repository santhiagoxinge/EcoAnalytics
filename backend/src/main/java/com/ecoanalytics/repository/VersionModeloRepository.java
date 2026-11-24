package com.ecoanalytics.repository;

import com.ecoanalytics.model.VersionModelo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VersionModeloRepository extends MongoRepository<VersionModelo, Long> {

    Optional<VersionModelo> findFirstByEstado(VersionModelo.Estado estado);

    List<VersionModelo> findAllByOrderByFechaEntrenamientoDesc();
}
