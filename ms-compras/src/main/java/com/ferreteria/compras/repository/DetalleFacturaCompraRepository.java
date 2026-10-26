package com.ferreteria.compras.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ferreteria.entity.compras.DetalleFacturaCompra;

/**
 * Repositorio de DetalleFacturaCompra (entidad asociativa del muchos-a-muchos FacturaCompra - Producto).
 * Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface DetalleFacturaCompraRepository extends BaseRepository<DetalleFacturaCompra, Long> {

    /** Suma de subtotales de una factura; {@code null} si todavia no tiene detalles. */
    @Query("select sum(d.subtotal) from DetalleFacturaCompra d where d.facturaCompra.idFacturaCompra = :idFactura")
    BigDecimal sumSubtotalByFactura(@Param("idFactura") Long idFactura);
}
