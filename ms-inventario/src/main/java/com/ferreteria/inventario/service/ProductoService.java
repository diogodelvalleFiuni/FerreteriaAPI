package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.ProductoDTO;
import com.ferreteria.inventario.filter.ProductoFilter;

/** Operaciones de negocio de Producto. La implementacion esta en service/impl. */
public interface ProductoService extends CrudService<ProductoDTO, ProductoFilter, Long> {
}
