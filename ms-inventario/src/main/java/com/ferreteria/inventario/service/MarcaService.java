package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.MarcaDTO;
import com.ferreteria.inventario.filter.MarcaFilter;

/** Operaciones de negocio de Marca. La implementacion esta en service/impl. */
public interface MarcaService extends CrudService<MarcaDTO, MarcaFilter, Long> {
}
