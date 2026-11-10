package com.ferreteria.seguridad.dto;

import java.util.List;

/** Quien es el usuario del token y con que roles (sin prefijo ROLE_). */
public record MeResponse(String username, List<String> roles) {
}
