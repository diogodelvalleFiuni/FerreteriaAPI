package com.ferreteria.ventas.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.PresupuestoDetalleDTO;
import com.ferreteria.ventas.filter.PresupuestoDetalleFilter;
import com.ferreteria.ventas.service.PresupuestoDetalleService;

@RestController
@RequestMapping("/api/presupuestos-detalle")
@Tag(name = "Detalles de presupuesto", description = "Productos, cantidades y precios de cada presupuesto (muchos-a-muchos Presupuesto-Producto)")
public class PresupuestoDetalleController
        extends AbstractBaseController<PresupuestoDetalleDTO, PresupuestoDetalleFilter, Long, PresupuestoDetalleService> {

    public PresupuestoDetalleController(PresupuestoDetalleService service) {
        super(service);
    }
}
