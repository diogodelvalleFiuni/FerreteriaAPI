package com.ferreteria.apigateway.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ferreteria.apigateway.error.ApiErrorWriter;

import io.jsonwebtoken.JwtException;

/**
 * Exige un token "Authorization: Bearer ..." valido en todas las rutas, salvo las publicas
 * ({@code security.jwt.public-paths}). Se desactiva con {@code security.jwt.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtGatewayFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayFilter.class);

    private final JwtService jwt;
    private final ApiErrorWriter errors;
    private final List<String> publicPaths;

    public JwtGatewayFilter(JwtService jwt, ApiErrorWriter errors,
                            @Value("${security.jwt.public-paths}") List<String> publicPaths) {
        this.jwt = jwt;
        this.errors = errors;
        this.publicPaths = publicPaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return publicPaths.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("401 sin token: {} {}", req.getMethod(), req.getRequestURI());
            errors.write(res, HttpServletResponse.SC_UNAUTHORIZED, req.getRequestURI());
            return;
        }

        try {
            jwt.parse(header.substring(7));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("401 token invalido: {} {} ({})", req.getMethod(), req.getRequestURI(), ex.getMessage());
            errors.write(res, HttpServletResponse.SC_UNAUTHORIZED, req.getRequestURI());
            return;
        }
        chain.doFilter(req, res);
    }
}
