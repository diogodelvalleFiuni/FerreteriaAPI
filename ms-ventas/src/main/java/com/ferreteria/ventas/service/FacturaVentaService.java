package com.ferreteria.ventas.service;

import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.ventas.filter.FacturaVentaFilter;

/** Operaciones de negocio de FacturaVenta (borrado logico). La implementacion esta en service/impl. */
public interface FacturaVentaService extends CrudService<FacturaVentaDTO, FacturaVentaFilter, Long> {
}
