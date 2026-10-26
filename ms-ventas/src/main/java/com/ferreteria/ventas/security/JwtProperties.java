package com.ferreteria.ventas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuracion compartida del token (prefijo {@code app.security.jwt}). Los mismos valores
 * de {@code secret} y {@code algorithm} deben estar configurados en la API que emite el token.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("false") boolean secretBase64,
        @DefaultValue("HS256") String algorithm,
        @DefaultValue("") String issuer,
        @DefaultValue("roles") String rolesClaim) {
}
