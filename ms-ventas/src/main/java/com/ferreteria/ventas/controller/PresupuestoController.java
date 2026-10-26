package com.ferreteria.ventas.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.ventas.filter.PresupuestoFilter;
import com.ferreteria.ventas.service.PresupuestoService;

@RestController
@RequestMapping("/api/presupuestos")
@Tag(name = "Presupuestos", description = "Presupuestos de venta (el DELETE los pasa a estado inactivo)")
public class PresupuestoController
        extends AbstractBaseController<PresupuestoDTO, PresupuestoFilter, Long, PresupuestoService> {

    public PresupuestoController(PresupuestoService service) {
        super(service);
    }
}
