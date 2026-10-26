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

import com.ferreteria.commons.dto.MarcaDTO;
import com.ferreteria.inventario.filter.MarcaFilter;
import com.ferreteria.inventario.service.MarcaService;

@RestController
@RequestMapping("/api/marcas")
@Tag(name = "Marcas", description = "Marcas de productos")
public class MarcaController
        extends AbstractBaseController<MarcaDTO, MarcaFilter, Long, MarcaService> {

    public MarcaController(MarcaService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<MarcaDTO> search(@ParameterObject MarcaFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
