package com.ferreteria.inventario.repository;

import com.ferreteria.entity.general.Producto;

/**
 * Repositorio de Producto. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface ProductoRepository extends BaseRepository<Producto, Long> {
}
