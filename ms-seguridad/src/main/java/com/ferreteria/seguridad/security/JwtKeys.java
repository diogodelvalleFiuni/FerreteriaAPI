package com.ferreteria.seguridad.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

/** Misma resolucion de algoritmo y clave (y mismas validaciones) que JwtConfig de ms-inventario. */
final class JwtKeys {

    private JwtKeys() {
    }

    static MacAlgorithm resolveAlgorithm(String name) {
        MacAlgorithm algorithm = MacAlgorithm.from(name == null ? "" : name.trim().toUpperCase());
        if (algorithm == null) {
            throw new IllegalStateException("Algoritmo JWT no soportado: " + name + " (use HS256, HS384 o HS512)");
        }
        return algorithm;
    }

    /** Clave de los access token (la misma que validan ms-inventario y ms-cache-redis). */
    static SecretKey secretKey(JwtProperties props, MacAlgorithm algorithm) {
        return build(props.secret(), props.secretBase64(), algorithm, "app.security.jwt.secret (variable JWT_SECRET)");
    }

    /** Clave de los refresh token: obligatoria y DISTINTA de la de los access token. */
    static SecretKey refreshSecretKey(JwtProperties props, MacAlgorithm algorithm) {
        SecretKey refresh = build(props.refreshSecret(), props.secretBase64(), algorithm,
                "app.security.jwt.refresh-secret (variable JWT_REFRESH_SECRET)");
        SecretKey access = secretKey(props, algorithm);
        if (MessageDigest.isEqual(access.getEncoded(), refresh.getEncoded())) {
            throw new IllegalStateException(
                    "JWT_REFRESH_SECRET debe ser distinta de JWT_SECRET (si no, un refresh token valdria como access token)");
        }
        return refresh;
    }

    private static SecretKey build(String secret, boolean base64, MacAlgorithm algorithm, String propertyName) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta " + propertyName);
        }
        byte[] keyBytes = base64
                ? Base64.getDecoder().decode(secret.trim())
                : secret.getBytes(StandardCharsets.UTF_8);
        int minBytes = switch (algorithm) {
            case HS384 -> 48;
            case HS512 -> 64;
            default -> 32;
        };
        if (keyBytes.length < minBytes) {
            throw new IllegalStateException("La clave de " + propertyName + " es demasiado corta para "
                    + algorithm.getName() + ": minimo " + minBytes + " bytes, tiene " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "Hmac" + algorithm.getName().replace("HS", "SHA"));
    }
}
