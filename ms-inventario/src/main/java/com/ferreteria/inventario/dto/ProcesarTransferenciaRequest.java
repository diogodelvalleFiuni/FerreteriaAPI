package com.ferreteria.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Pedido para procesar una transferencia completa: cabecera + productos con su cantidad.
 * El creador no viaja aca: se toma del token JWT.
 */
public record ProcesarTransferenciaRequest(
        @NotNull Long idDepositoOrigen,
        @NotNull Long idDepositoDestino,
        LocalDateTime fechaEstimada,
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull Long idProducto,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal cantidad) {
    }
}
