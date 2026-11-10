package com.ferreteria.inventario.repository;

import com.ferreteria.entity.Inventario.Inventario;

/**
 * Repositorio de Inventario. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface InventarioRepository extends BaseRepository<Inventario, Long> {
}
