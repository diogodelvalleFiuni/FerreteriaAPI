package com.ferreteria.ventas.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.ventas.filter.FacturaVentaFilter;
import com.ferreteria.ventas.service.FacturaVentaService;

@RestController
@RequestMapping("/api/facturas-ventas")
@Tag(name = "Facturas de venta", description = "Facturas de venta (el DELETE las pasa a estado inactivo)")
public class FacturaVentaController
        extends AbstractBaseController<FacturaVentaDTO, FacturaVentaFilter, Long, FacturaVentaService> {

    public FacturaVentaController(FacturaVentaService service) {
        super(service);
    }
}
