package com.ferreteria.ventas.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.DetalleFacturaVentaDTO;
import com.ferreteria.ventas.filter.DetalleFacturaVentaFilter;
import com.ferreteria.ventas.service.DetalleFacturaVentaService;

@RestController
@RequestMapping("/api/detalles-facturas-ventas")
@Tag(name = "Detalles de factura de venta", description = "Productos, cantidades y precios de cada factura (muchos-a-muchos FacturaVenta-Producto)")
public class DetalleFacturaVentaController
        extends AbstractBaseController<DetalleFacturaVentaDTO, DetalleFacturaVentaFilter, Long, DetalleFacturaVentaService> {

    public DetalleFacturaVentaController(DetalleFacturaVentaService service) {
        super(service);
    }
}
