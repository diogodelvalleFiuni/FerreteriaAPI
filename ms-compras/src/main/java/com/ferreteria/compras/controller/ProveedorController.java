package com.ferreteria.compras.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.ProveedorDTO;
import com.ferreteria.compras.filter.ProveedorFilter;
import com.ferreteria.compras.service.ProveedorService;

@RestController
@RequestMapping("/api/proveedores")
@Tag(name = "Proveedores", description = "Proveedores de la ferreteria")
public class ProveedorController
        extends AbstractBaseController<ProveedorDTO, ProveedorFilter, Long, ProveedorService> {

    public ProveedorController(ProveedorService service) {
        super(service);
    }
}
