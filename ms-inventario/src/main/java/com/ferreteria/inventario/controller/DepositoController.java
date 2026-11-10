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

import com.ferreteria.commons.dto.DepositoDTO;
import com.ferreteria.inventario.filter.DepositoFilter;
import com.ferreteria.inventario.service.DepositoService;

@RestController
@RequestMapping("/api/depositos")
@Tag(name = "Depositos", description = "Depositos de la ferreteria")
public class DepositoController
        extends AbstractBaseController<DepositoDTO, DepositoFilter, Long, DepositoService> {

    public DepositoController(DepositoService service) {
        super(service);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<DepositoDTO> search(@ParameterObject DepositoFilter filter, @ParameterObject Pageable pageable) {
        return searchInternal(filter, pageable);
    }
}
