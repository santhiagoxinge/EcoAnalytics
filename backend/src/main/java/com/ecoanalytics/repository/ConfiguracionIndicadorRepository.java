package com.ecoanalytics.repository;

import com.ecoanalytics.model.ConfiguracionIndicador;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConfiguracionIndicadorRepository extends MongoRepository<ConfiguracionIndicador, String> {

    List<ConfiguracionIndicador> findByHabilitadoTrue();
}
