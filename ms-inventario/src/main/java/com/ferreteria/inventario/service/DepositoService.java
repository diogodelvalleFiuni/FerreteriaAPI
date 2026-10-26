package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.DepositoDTO;
import com.ferreteria.inventario.filter.DepositoFilter;

/** Operaciones de negocio de Deposito. La implementacion esta en service/impl. */
public interface DepositoService extends CrudService<DepositoDTO, DepositoFilter, Long> {
}
