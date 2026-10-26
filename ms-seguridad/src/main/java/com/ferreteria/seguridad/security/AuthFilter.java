package com.ferreteria.seguridad.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica las rutas protegidas de ESTE servicio (/api/auth/me, /api/usuarios).
 * A diferencia de los filtros de ms-inventario / ms-cache-redis, aca la base de datos es propia, asi que ademas de
 * validar el access token se vuelve a leer el usuario: si lo deshabilitaron o le cambiaron los roles, el cambio
 * rige de inmediato (no hay que esperar a que venza el token).
 * No es @Component a proposito: se agrega solo en SecurityConfig (si no, se registraria dos veces).
 */
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtDecoder accessDecoder;
    private final UserDetailsService userDetailsService;
    private final RestSecurityErrorHandler errorHandler;

    public AuthFilter(JwtDecoder accessDecoder, UserDetailsService userDetailsService,
            RestSecurityErrorHandler errorHandler) {
        this.accessDecoder = accessDecoder;
        this.userDetailsService = userDetailsService;
        this.errorHandler = errorHandler;
    }

    /** Login y refresh son publicos: un header Authorization viejo o vencido no debe impedirlos. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.POST.matches(request.getMethod())
                && ("/api/auth/login".equals(path) || "/api/auth/refresh".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(JwtSpec.AUTH_HEADER);
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.startsWith(JwtSpec.BEARER_PREFIX)) {
            reject(request, response, "Header Authorization sin esquema Bearer");
            return;
        }
        String token = header.substring(JwtSpec.BEARER_PREFIX.length()).trim();
        if (!JwtSpec.hasValidFormat(token)) {
            reject(request, response, "Token malformado: se esperan 3 secciones separadas por puntos");
            return;
        }

        String username;
        try {
            username = accessDecoder.decode(token).getSubject();
        } catch (JwtException ex) {
            reject(request, response, "Token rechazado: " + ex.getMessage());
            return;
        }
        if (username == null || username.isBlank()) {
            reject(request, response, "El token no tiene usuario (sub)");
            return;
        }

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            reject(request, response, "El usuario del token ya no existe");
            return;
        } catch (RuntimeException ex) {
            log.error("500 no se pudo leer el usuario '{}' (base de datos?)", username, ex);
            SecurityContextHolder.clearContext();
            errorHandler.write(request, response, HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
            return;
        }
        if (!user.isEnabled()) {
            reject(request, response, "El usuario del token esta deshabilitado");
            return;
        }

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith(ROLE_PREFIX) ? a.substring(ROLE_PREFIX.length()) : a)
                .toList();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(user.getUsername(), roles), null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String logMessage)
            throws IOException {
        log.warn("401 {}: {}", request.getRequestURI(), logMessage); // el detalle solo se loguea
        SecurityContextHolder.clearContext();
        errorHandler.write(request, response, HttpStatus.UNAUTHORIZED, "Autenticacion requerida o token invalido");
    }
}
