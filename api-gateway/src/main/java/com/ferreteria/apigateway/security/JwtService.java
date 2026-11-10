package com.ferreteria.apigateway.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Valida tokens JWT firmados con HMAC (el secret se comparte con el servicio de autenticacion). */
@Service
@ConditionalOnProperty(prefix = "security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtService {

    private final Key key;

    public JwtService(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Valida firma, estructura y expiracion. Lanza {@link io.jsonwebtoken.JwtException} si es invalido. */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
