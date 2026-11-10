package com.ferreteria.seguridad.dto;

/**
 * @param token             access token (el que se manda en Authorization: Bearer)
 * @param expiresIn         segundos de vida del access token
 * @param refreshToken      solo sirve en POST /api/auth/refresh
 * @param refreshExpiresIn  segundos de vida del refresh token
 */
public record LoginResponse(String token, String tokenType, long expiresIn, String refreshToken,
        long refreshExpiresIn) {

    /** Los tokens no deben aparecer si el objeto se loguea por error. */
    @Override
    public String toString() {
        return "LoginResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", tokens=****]";
    }
}
