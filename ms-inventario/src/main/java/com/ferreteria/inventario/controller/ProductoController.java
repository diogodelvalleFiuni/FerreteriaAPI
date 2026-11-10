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

import com.ferreteria.commons.dto.ProductoDTO;
import com.ferreteria.inventario.filter.ProductoFilter;
import com.ferreteria.inventario.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catalogo de productos")
public class ProductoController
        extends AbstractBaseController<ProductoDTO, ProductoFilter, Long, ProductoService> {

    public ProductoController(ProductoService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<ProductoDTO> search(@ParameterObject ProductoFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
