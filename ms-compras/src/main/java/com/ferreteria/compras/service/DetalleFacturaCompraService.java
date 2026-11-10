package com.ferreteria.compras.service;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.compras.filter.DetalleFacturaCompraFilter;

/** Operaciones de negocio de DetalleFacturaCompra. La implementacion esta en service/impl. */
public interface DetalleFacturaCompraService
        extends CrudService<DetalleFacturaCompraDTO, DetalleFacturaCompraFilter, Long> {
}
