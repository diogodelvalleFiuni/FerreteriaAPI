package com.ferreteria.seguridad.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.ferreteria.seguridad.exception.ApiException;

/**
 * Reglas minimas para contrasenas nuevas: largo (lo que mas pesa) y limite de BCrypt (72 bytes: lo que
 * pasa de ahi se ignoraria o haria fallar al encoder). No hay reglas de "una mayuscula y un simbolo".
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** @throws ApiException 400 si no cumple */
    public static void validate(String password, String username) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contrasena debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contrasena no puede superar " + MAX_BYTES + " bytes");
        }
        if (password.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contrasena no puede estar en blanco");
        }
        if (username != null && username.length() >= 3
                && password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contrasena no puede contener el nombre de usuario");
        }
    }
}
