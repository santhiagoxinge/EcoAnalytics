package com.ecoanalytics.repository;

import com.ecoanalytics.model.ConfiguracionSistema;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfiguracionSistemaRepository extends MongoRepository<ConfiguracionSistema, String> {
}
