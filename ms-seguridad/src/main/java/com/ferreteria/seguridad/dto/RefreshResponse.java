package com.ferreteria.seguridad.dto;

/** Nuevo access token. El refresh token no se rota: sigue siendo el mismo hasta que venza. */
public record RefreshResponse(String token, String tokenType, long expiresIn) {

    @Override
    public String toString() {
        return "RefreshResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", token=****]";
    }
}
