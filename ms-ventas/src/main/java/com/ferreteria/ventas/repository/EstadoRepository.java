package com.ferreteria.ventas.repository;

import com.ferreteria.entity.general.Estado;

import java.util.Optional;

/**
 * Repositorio de Estado. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface EstadoRepository extends BaseRepository<Estado, Long> {

    /** Busca un estado por nombre sin distinguir mayusculas (p. ej. el estado inactivo). */
    Optional<Estado> findFirstByNombreIgnoreCase(String nombre);
}
