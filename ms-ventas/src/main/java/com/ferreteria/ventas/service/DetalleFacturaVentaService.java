package com.ferreteria.ventas.service;

import com.ferreteria.commons.dto.DetalleFacturaVentaDTO;
import com.ferreteria.ventas.filter.DetalleFacturaVentaFilter;

/** Operaciones de negocio de DetalleFacturaVenta. La implementacion esta en service/impl. */
public interface DetalleFacturaVentaService extends CrudService<DetalleFacturaVentaDTO, DetalleFacturaVentaFilter, Long> {
}
