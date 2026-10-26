package com.ferreteria.apigateway.error;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Red de seguridad del gateway: cualquier excepcion que escape del ruteo (servicio sin instancias en
 * Eureka, servicio caido, timeout, error inesperado) se transforma en una respuesta {@link ApiError}
 * estandar. El detalle tecnico se registra en el log y nunca se devuelve al cliente.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiErrorFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorFilter.class);

    private final ApiErrorWriter writer;

    public ApiErrorFilter(ApiErrorWriter writer) {
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } catch (ServletException | IOException | RuntimeException ex) {
            if (res.isCommitted()) {
                throw ex;
            }
            int status = statusOf(ex);
            if (status >= 500) {
                log.error("{} {} -> {}", req.getMethod(), req.getRequestURI(), status, ex);
            } else {
                log.warn("{} {} -> {} ({})", req.getMethod(), req.getRequestURI(), status, ex.getMessage());
            }
            writer.write(res, status, req.getRequestURI());
        }
    }

    private static int statusOf(Throwable ex) {
        Throwable http = find(ex, HttpStatusCodeException.class);
        if (http != null) {
            return ((HttpStatusCodeException) http).getStatusCode().value();
        }
        if (find(ex, SocketTimeoutException.class) != null || find(ex, HttpTimeoutException.class) != null) {
            return 504;
        }
        if (find(ex, IOException.class) != null) {
            return 502;   // conexion rechazada, servicio caido, etc.
        }
        return 500;
    }

    private static Throwable find(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return t;
            }
        }
        return null;
    }
}
