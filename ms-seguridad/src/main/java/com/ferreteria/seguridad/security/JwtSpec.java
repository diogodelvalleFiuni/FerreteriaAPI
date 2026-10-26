package com.ferreteria.seguridad.security;

/** Header HTTP, prefijo y formato del token (igual que JwtSpec de ms-inventario). */
public final class JwtSpec {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int SECTIONS = 3;

    private JwtSpec() {
    }

    /** true si el texto tiene exactamente 3 secciones no vacias separadas por puntos. */
    public static boolean hasValidFormat(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != SECTIONS) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
