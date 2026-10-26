package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.inventario.filter.InventarioFilter;

/** Operaciones de negocio de Inventario. La implementacion esta en service/impl. */
public interface InventarioService extends CrudService<InventarioDTO, InventarioFilter, Long> {
}
