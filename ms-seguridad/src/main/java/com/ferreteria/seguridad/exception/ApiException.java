package com.ferreteria.seguridad.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con su codigo HTTP; GlobalExceptionHandler lo convierte en ApiErrorDTO. El mensaje es apto para el cliente. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
