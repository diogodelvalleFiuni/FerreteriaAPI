package com.ferreteria.inventario.controller;

import java.math.BigDecimal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.inventario.dto.AjusteStockRequest;
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

    @GetMapping("/stock")
    @Operation(summary = "Stock actual de un producto en un deposito (0 si no hay registro)")
    @ApiResponse(responseCode = "200", description = "Cantidad disponible")
    public BigDecimal consultarStock(@RequestParam Long idDeposito, @RequestParam Long idProducto) {
        log.info("GET stock deposito={} producto={}", idDeposito, idProducto);
        return service.consultarStock(idDeposito, idProducto);
    }

    @PostMapping("/incrementar")
    @Operation(summary = "Subir stock (crea el registro si el producto no estaba en el deposito)")
    @ApiResponse(responseCode = "200", description = "Inventario actualizado")
    @ApiResponse(responseCode = "422", description = "Deposito o producto inexistente")
    public InventarioDTO incrementar(@Valid @RequestBody AjusteStockRequest request) {
        log.info("POST incrementar stock: {}", request);
        return service.incrementarStock(request.idDeposito(), request.idProducto(), request.cantidad());
    }

    @PostMapping("/descontar")
    @Operation(summary = "Descontar stock")
    @ApiResponse(responseCode = "200", description = "Inventario actualizado")
    @ApiResponse(responseCode = "422", description = "Stock insuficiente o inexistente")
    public InventarioDTO descontar(@Valid @RequestBody AjusteStockRequest request) {
        log.info("POST descontar stock: {}", request);
        return service.descontarStock(request.idDeposito(), request.idProducto(), request.cantidad());
    }

    @DeleteMapping("/historico")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Purga simulada de historicos (propagacion NEVER)")
    @ApiResponse(responseCode = "204", description = "Purga ejecutada")
    public void purgarHistorico() {
        log.info("DELETE purga de historico");
        service.purgarHistorico();
    }
}
