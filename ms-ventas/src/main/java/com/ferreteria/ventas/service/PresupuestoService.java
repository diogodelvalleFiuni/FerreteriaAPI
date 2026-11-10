package com.ferreteria.ventas.service;

import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.ventas.filter.PresupuestoFilter;

/** Operaciones de negocio de Presupuesto (borrado logico). La implementacion esta en service/impl. */
public interface PresupuestoService extends CrudService<PresupuestoDTO, PresupuestoFilter, Long> {
}
