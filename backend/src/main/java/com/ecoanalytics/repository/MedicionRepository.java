package com.ecoanalytics.repository;

import com.ecoanalytics.model.EstadoMedicion;
import com.ecoanalytics.model.Medicion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la colección "mediciones". Los rangos de fecha se escriben con $gte/$lte porque
 * el "Between" derivado de Spring Data MongoDB excluye los extremos.
 */
public interface MedicionRepository extends MongoRepository<Medicion, Long> {

    /** Todas las mediciones de una estación en el rango, incluidas las descartadas (vista de auditoría). */
    @Query(value = "{ 'estacionId': ?0, 'fechaMedicion': { $gte: ?1, $lte: ?2 } }", sort = "{ 'fechaMedicion': 1 }")
    List<Medicion> buscarPorEstacionYRango(Long estacionId, LocalDateTime desde, LocalDateTime hasta);

    /** Mediciones aptas para el análisis: nunca las descartadas por la validación (regla de negocio 4). */
    @Query(value = "{ 'estacionId': ?0, 'fechaMedicion': { $gte: ?1, $lte: ?2 }, 'estado': { $ne: 'DESCARTADA' } }",
            sort = "{ 'fechaMedicion': 1 }")
    List<Medicion> buscarUtilesPorEstacionYRango(Long estacionId, LocalDateTime desde, LocalDateTime hasta);

    Optional<Medicion> findFirstByEstacionIdAndEstadoNotOrderByFechaMedicionDesc(Long estacionId, EstadoMedicion excluido);

    Optional<Medicion> findFirstByEstacionIdOrderByFechaMedicionDesc(Long estacionId);

    long countByFechaMedicionAfter(LocalDateTime desde);

    long countByEstado(EstadoMedicion estado);

    boolean existsByEstacionIdAndFechaMedicion(Long estacionId, LocalDateTime fechaMedicion);
}
