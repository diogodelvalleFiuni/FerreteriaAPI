package com.ferreteria.compras.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.compras.filter.FacturaCompraFilter;
import com.ferreteria.compras.service.FacturaCompraService;

@RestController
@RequestMapping("/api/facturas-compras")
@Tag(name = "Facturas de compra", description = "Facturas de compra a proveedores (DELETE = borrado logico)")
public class FacturaCompraController
        extends AbstractBaseController<FacturaCompraDTO, FacturaCompraFilter, Long, FacturaCompraService> {

    public FacturaCompraController(FacturaCompraService service) {
        super(service);
    }
}
