package com.ferreteria.inventario.exception;

/**
 * Regla de negocio incumplida (stock insuficiente, deposito inexistente, cantidad invalida...).
 * Se traduce a HTTP 422 en el {@link GlobalExceptionHandler}. Al ser RuntimeException, dentro de una
 * operacion {@code @Transactional} provoca rollback.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
