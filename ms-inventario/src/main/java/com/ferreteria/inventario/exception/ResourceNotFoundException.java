package com.ferreteria.inventario.exception;

/**
 * Se lanza cuando se busca una entidad por id y no existe (o no esta activa).
 * Pensada para ser traducida a HTTP 404 en un {@code @ControllerAdvice}.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
