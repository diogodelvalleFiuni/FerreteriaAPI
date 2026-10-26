package com.ferreteria.seguridad.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Mismo prefijo ({@code app.security.jwt}) y mismos nombres que {@code JwtProperties} de ms-inventario.
 * {@code secret}, {@code secretBase64}, {@code algorithm}, {@code issuer} y {@code rolesClaim} DEBEN coincidir
 * con ms-inventario y ms-cache-redis; {@code expirationMinutes}, {@code refreshSecret} y
 * {@code refreshExpirationDays} solo existen aca (quien emite).
 *
 * <p>{@code refreshSecret} firma los refresh token con una clave DISTINTA de {@code secret}. Asi los demas
 * microservicios (que solo conocen {@code secret}) jamas aceptan un refresh token como si fuera un access token.
 * Usa el mismo formato (texto o Base64, segun {@code secretBase64}) y el mismo largo minimo que {@code secret}.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("false") boolean secretBase64,
        @DefaultValue("HS256") String algorithm,
        @DefaultValue("") String issuer,
        @DefaultValue("roles") String rolesClaim,
        @DefaultValue("60") long expirationMinutes,
        String refreshSecret,
        @DefaultValue("7") long refreshExpirationDays) {
}
