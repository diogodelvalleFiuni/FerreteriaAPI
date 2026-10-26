package com.ferreteria.ventas.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Arma el {@link JwtDecoder} de Spring con la clave y el algoritmo compartidos.
 * Valida firma, expiracion/not-before y, si se configuro, el emisor ("iss").
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties props) {
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException("Falta app.security.jwt.secret (variable JWT_SECRET)");
        }
        MacAlgorithm algorithm = resolveAlgorithm(props.algorithm());
        byte[] keyBytes = props.secretBase64()
                ? Base64.getDecoder().decode(props.secret().trim())
                : props.secret().getBytes(StandardCharsets.UTF_8);
        int minBytes = switch (algorithm) {
            case HS384 -> 48;
            case HS512 -> 64;
            default -> 32;
        };
        if (keyBytes.length < minBytes) {
            throw new IllegalStateException("La clave JWT es demasiado corta para " + algorithm.getName()
                    + ": minimo " + minBytes + " bytes, tiene " + keyBytes.length);
        }
        SecretKey key = new SecretKeySpec(keyBytes, "Hmac" + algorithm.getName().replace("HS", "SHA"));

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(algorithm).build();
        OAuth2TokenValidator<Jwt> validator = props.issuer() == null || props.issuer().isBlank()
                ? JwtValidators.createDefault()
                : JwtValidators.createDefaultWithIssuer(props.issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validator));
        return decoder;
    }

    private static MacAlgorithm resolveAlgorithm(String name) {
        MacAlgorithm algorithm = MacAlgorithm.from(name == null ? "" : name.trim().toUpperCase());
        if (algorithm == null) {
            throw new IllegalStateException("Algoritmo JWT no soportado: " + name + " (use HS256, HS384 o HS512)");
        }
        return algorithm;
    }
}
