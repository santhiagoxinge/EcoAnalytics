package com.ecoanalytics.repository;

import com.ecoanalytics.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, Long> {

    /** El correo se guarda siempre en minúsculas, así que la búsqueda es exacta. */
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    List<Usuario> findAllByOrderByNombreCompletoAsc();

    @Query(value = "{ $or: [ { 'nombreCompleto': { $regex: ?0, $options: 'i' } }, { 'correo': { $regex: ?0, $options: 'i' } } ] }",
            sort = "{ 'nombreCompleto': 1 }")
    List<Usuario> buscarPorNombreOCorreo(String patron);
}
