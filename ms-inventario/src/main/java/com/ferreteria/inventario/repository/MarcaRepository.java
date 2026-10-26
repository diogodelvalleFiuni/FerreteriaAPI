package com.ferreteria.inventario.repository;

import com.ferreteria.entity.general.Marca;

/**
 * Repositorio de Marca. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface MarcaRepository extends BaseRepository<Marca, Long> {
}
