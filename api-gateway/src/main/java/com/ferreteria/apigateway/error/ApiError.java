package com.ferreteria.apigateway.error;

/**
 * Cuerpo estandar de error. Mantiene los mismos campos que el ApiErrorDTO que devuelve
 * ms-inventario (timestamp, status, error, message, path), para que el cliente reciba
 * siempre el mismo formato, venga el error del gateway o de un microservicio.
 */
public record ApiError(String timestamp, int status, String error, String message, String path) {
}
