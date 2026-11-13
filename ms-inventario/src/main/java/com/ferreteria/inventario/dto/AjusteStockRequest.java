package com.ferreteria.inventario.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Pedido para subir o descontar stock de un producto en un deposito. */
public record AjusteStockRequest(
        @NotNull Long idDeposito,
        @NotNull Long idProducto,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal cantidad) {
}
