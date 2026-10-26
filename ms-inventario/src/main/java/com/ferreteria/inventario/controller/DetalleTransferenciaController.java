package com.ferreteria.inventario.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.inventario.filter.DetalleTransferenciaFilter;
import com.ferreteria.inventario.service.DetalleTransferenciaService;

@RestController
@RequestMapping("/api/detalles-transferencia")
@Tag(name = "Detalles de transferencia", description = "Items (producto y cantidad) de cada transferencia")
public class DetalleTransferenciaController
        extends AbstractBaseController<DetalleTransferenciaDTO, DetalleTransferenciaFilter, Long, DetalleTransferenciaService> {

    public DetalleTransferenciaController(DetalleTransferenciaService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<DetalleTransferenciaDTO> search(@ParameterObject DetalleTransferenciaFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
