package com.ferreteria.ventas.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepta cada peticion: toma el token del header {@code Authorization: Bearer ...}, comprueba el formato
 * de 3 secciones, lo valida con el {@link JwtDecoder} (firma, expiracion, emisor) y publica en el
 * {@link SecurityContext} un {@link UsuarioPrincipal} con los datos del token.
 * <ul>
 *     <li>Sin header: sigue la cadena sin autenticar; si la ruta es protegida, Spring Security responde 401.</li>
 *     <li>Token presente pero malformado/invalido/vencido: responde 401 sin llegar al controller.</li>
 * </ul>
 * No es un {@code @Component} a proposito: se agrega solo a la cadena de seguridad (SecurityConfig),
 * asi no se registra una segunda vez como filtro del contenedor.
 */
public class AuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;
    private final RestSecurityErrorHandler errorHandler;

    public AuthFilter(JwtDecoder jwtDecoder, JwtProperties properties, RestSecurityErrorHandler errorHandler) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
        this.errorHandler = errorHandler;
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
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException ex) {
            // el detalle (firma, vencido, etc.) solo se loguea, no se le muestra al cliente
            reject(request, response, "Token rechazado: " + ex.getMessage());
            return;
        }

        UsuarioPrincipal principal = toPrincipal(jwt);
        List<GrantedAuthority> authorities = principal.roles().stream()
                .map(r -> r.startsWith(ROLE_PREFIX) ? r : ROLE_PREFIX + r)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, token, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        log.debug("Autenticado '{}' con roles {}", principal.username(), principal.roles());
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String logMessage)
            throws IOException {
        log.warn("401 {}: {}", request.getRequestURI(), logMessage);
        SecurityContextHolder.clearContext();
        errorHandler.write(request, response, HttpStatus.UNAUTHORIZED, "Autenticacion requerida o token invalido");
    }

    /** Datos del token validado -> Principal. El claim de roles puede ser una lista o un texto separado por espacios. */
    private UsuarioPrincipal toPrincipal(Jwt jwt) {
        Object raw = jwt.getClaim(properties.rolesClaim());
        List<String> roles;
        if (raw instanceof Collection<?> collection) {
            roles = collection.stream().map(String::valueOf).filter(r -> !r.isBlank()).toList();
        } else if (raw instanceof String text && !text.isBlank()) {
            roles = List.of(text.trim().split("\\s+"));
        } else {
            roles = List.of();
        }
        return new UsuarioPrincipal(jwt.getSubject(), roles);
    }
}
