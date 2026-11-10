package com.ferreteria.ventas.exception;

import org.springframework.http.HttpStatus;

/**
 * Violacion de una regla de negocio (dato incoherente, duplicado, etc.).
 * El {@link GlobalExceptionHandler} la traduce al codigo HTTP indicado (400 por defecto).
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
