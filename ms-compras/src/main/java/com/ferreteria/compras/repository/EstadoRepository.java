package com.ferreteria.compras.repository;

import java.util.Optional;

import com.ferreteria.entity.general.Estado;

/**
 * Repositorio de apoyo (sin controller): resuelve los estados "activo" / "inactivo" usados en el borrado logico.
 */
public interface EstadoRepository extends BaseRepository<Estado, Long> {

    Optional<Estado> findFirstByNombreIgnoreCase(String nombre);
}
