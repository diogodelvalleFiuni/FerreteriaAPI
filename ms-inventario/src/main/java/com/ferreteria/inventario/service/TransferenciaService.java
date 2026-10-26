package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.inventario.filter.TransferenciaFilter;

/** Operaciones de negocio de Transferencia. La implementacion esta en service/impl. */
public interface TransferenciaService extends CrudService<TransferenciaDTO, TransferenciaFilter, Long> {
}
