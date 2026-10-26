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

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.inventario.filter.TransferenciaFilter;
import com.ferreteria.inventario.service.TransferenciaService;

@RestController
@RequestMapping("/api/transferencias")
@Tag(name = "Transferencias", description = "Transferencias de mercaderia entre depositos")
public class TransferenciaController
        extends AbstractBaseController<TransferenciaDTO, TransferenciaFilter, Long, TransferenciaService> {

    public TransferenciaController(TransferenciaService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<TransferenciaDTO> search(@ParameterObject TransferenciaFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
