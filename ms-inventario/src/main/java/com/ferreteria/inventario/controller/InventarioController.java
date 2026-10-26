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

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.inventario.filter.InventarioFilter;
import com.ferreteria.inventario.service.InventarioService;

@RestController
@RequestMapping("/api/inventarios")
@Tag(name = "Inventarios", description = "Stock de cada producto por deposito")
public class InventarioController
        extends AbstractBaseController<InventarioDTO, InventarioFilter, Long, InventarioService> {

    public InventarioController(InventarioService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<InventarioDTO> search(@ParameterObject InventarioFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
