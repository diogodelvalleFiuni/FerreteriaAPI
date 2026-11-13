package com.ferreteria.inventario.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.inventario.dto.ProcesarTransferenciaRequest;
import com.ferreteria.inventario.dto.TransferenciaProcesadaDTO;
import com.ferreteria.inventario.filter.TransferenciaFilter;
import com.ferreteria.inventario.security.UsuarioPrincipal;
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

    @PostMapping("/procesar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Procesar una transferencia: guarda cabecera y detalles, descuenta del origen y suma en el destino")
    @ApiResponse(responseCode = "201", description = "Transferencia procesada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "422", description = "Stock insuficiente, deposito inexistente u origen igual al destino")
    public TransferenciaProcesadaDTO procesar(@Valid @RequestBody ProcesarTransferenciaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        String creador = usuario != null ? usuario.username() : null;
        log.info("POST procesar transferencia {} -> {} por {}", request.idDepositoOrigen(), request.idDepositoDestino(), creador);
        log.debug("POST procesar transferencia, cuerpo: {}", request);
        return service.procesarTransferencia(request, creador);
    }
}
