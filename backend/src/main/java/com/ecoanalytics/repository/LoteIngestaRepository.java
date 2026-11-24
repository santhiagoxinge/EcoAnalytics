package com.ecoanalytics.repository;

import com.ecoanalytics.model.LoteIngesta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LoteIngestaRepository extends MongoRepository<LoteIngesta, Long> {

    List<LoteIngesta> findTop20ByOrderByFechaCreacionDesc();
}
