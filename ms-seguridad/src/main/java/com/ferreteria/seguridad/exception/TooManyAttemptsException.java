package com.ferreteria.seguridad.exception;

/** Login bloqueado temporalmente por demasiados intentos fallidos (HTTP 429). */
public class TooManyAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyAttemptsException(long retryAfterSeconds) {
        super("Demasiados intentos fallidos");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
