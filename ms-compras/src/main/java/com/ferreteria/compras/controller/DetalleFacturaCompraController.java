package com.ferreteria.compras.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.compras.filter.DetalleFacturaCompraFilter;
import com.ferreteria.compras.service.DetalleFacturaCompraService;

@RestController
@RequestMapping("/api/detalles-compras")
@Tag(name = "Detalles de compra", description = "Lineas de las facturas de compra (relacion factura - producto)")
public class DetalleFacturaCompraController
        extends AbstractBaseController<DetalleFacturaCompraDTO, DetalleFacturaCompraFilter, Long, DetalleFacturaCompraService> {

    public DetalleFacturaCompraController(DetalleFacturaCompraService service) {
        super(service);
    }
}
