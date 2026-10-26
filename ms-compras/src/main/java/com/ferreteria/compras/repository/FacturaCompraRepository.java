package com.ferreteria.compras.repository;

import java.math.BigDecimal;

import com.ferreteria.entity.compras.FacturaCompra;

/**
 * Repositorio de FacturaCompra. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface FacturaCompraRepository extends BaseRepository<FacturaCompra, Long> {

    /** Otra factura (distinta de {@code idFacturaCompra}) del mismo proveedor con igual numero y timbrado. */
    boolean existsByProveedor_IdPersonaAndNroFacturaAndTimbradoAndIdFacturaCompraNot(
            Long idProveedor, String nroFactura, BigDecimal timbrado, Long idFacturaCompra);

    /** Igual que la anterior para facturas sin timbrado (compara solo proveedor y numero). */
    boolean existsByProveedor_IdPersonaAndNroFacturaAndIdFacturaCompraNot(
            Long idProveedor, String nroFactura, Long idFacturaCompra);
}
