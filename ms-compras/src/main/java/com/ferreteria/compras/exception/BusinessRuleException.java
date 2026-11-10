package com.ferreteria.compras.exception;

/**
 * Se lanza cuando una operacion viola una regla de negocio (ej. fecha de vencimiento anterior a la
 * emision, porcentaje de IVA no permitido). Se traduce a HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
