package com.ferreteria.compras.service;

import java.math.BigDecimal;

import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.compras.filter.FacturaCompraFilter;

/** Operaciones de negocio de FacturaCompra. La implementacion esta en service/impl. */
public interface FacturaCompraService extends CrudService<FacturaCompraDTO, FacturaCompraFilter, Long> {

    /**
     * Fija el importe total de la factura. Lo invoca el servicio de detalles cada vez que cambian las lineas
     * (el importe no se acepta desde el cliente).
     *
     * @throws com.ferreteria.compras.exception.ResourceNotFoundException si la factura no existe o esta inactiva
     */
    void actualizarImporteTotal(Long idFacturaCompra, BigDecimal importeTotal);
}
