package com.ferreteria.seguridad.security;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.ferreteria.commons.dto.ApiErrorDTO;

import tools.jackson.databind.json.JsonMapper;

/** 401/403 con el mismo ApiErrorDTO que ms-inventario (las excepciones de seguridad ocurren antes de los controllers). */
@Component
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public RestSecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "Autenticacion requerida o token invalido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "No tiene permisos para acceder a este recurso");
    }

    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ApiErrorDTO body = new ApiErrorDTO()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
