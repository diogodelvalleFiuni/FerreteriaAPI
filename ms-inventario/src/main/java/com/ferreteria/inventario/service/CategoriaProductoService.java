package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.CategoriaProductoDTO;
import com.ferreteria.inventario.filter.CategoriaProductoFilter;

/** Operaciones de negocio de CategoriaProducto. La implementacion esta en service/impl. */
public interface CategoriaProductoService extends CrudService<CategoriaProductoDTO, CategoriaProductoFilter, Long> {
}
