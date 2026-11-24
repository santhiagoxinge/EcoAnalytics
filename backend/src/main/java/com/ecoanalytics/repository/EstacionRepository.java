package com.ecoanalytics.repository;

import com.ecoanalytics.model.Estacion;
import com.ecoanalytics.model.EstadoEstacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EstacionRepository extends MongoRepository<Estacion, Long> {

    List<Estacion> findAllByOrderByNombreAsc();

    List<Estacion> findByEstado(EstadoEstacion estado);

    long countByEstado(EstadoEstacion estado);
}
