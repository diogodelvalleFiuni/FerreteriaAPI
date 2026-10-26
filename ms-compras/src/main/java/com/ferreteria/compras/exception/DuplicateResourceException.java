package com.ferreteria.compras.exception;

/**
 * Se lanza cuando se intenta crear o modificar un registro que duplica a otro existente
 * (ej. misma factura del mismo proveedor). Se traduce a HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(message);
    }
}
