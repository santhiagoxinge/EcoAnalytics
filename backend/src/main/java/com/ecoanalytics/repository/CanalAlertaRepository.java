package com.ecoanalytics.repository;

import com.ecoanalytics.model.CanalAlerta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CanalAlertaRepository extends MongoRepository<CanalAlerta, String> {

    List<CanalAlerta> findByHabilitadoTrue();

    List<CanalAlerta> findAllByOrderByCodigoAsc();
}
