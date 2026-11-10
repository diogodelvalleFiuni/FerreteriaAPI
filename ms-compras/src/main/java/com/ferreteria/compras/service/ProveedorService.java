package com.ferreteria.compras.service;

import com.ferreteria.commons.dto.ProveedorDTO;
import com.ferreteria.compras.filter.ProveedorFilter;

/** Operaciones de negocio de Proveedor. La implementacion esta en service/impl. */
public interface ProveedorService extends CrudService<ProveedorDTO, ProveedorFilter, Long> {
}
