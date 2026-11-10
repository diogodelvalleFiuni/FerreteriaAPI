package com.ferreteria.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 72) String password) { // 72 = limite de BCrypt: mas largo nunca puede ser valido

    /** Evita que la contrasena aparezca si el objeto se loguea por error. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=****]";
    }
}
