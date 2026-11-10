package com.ferreteria.seguridad.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferreteria.commons.dto.ApiErrorDTO;

/** Mismo formato (ApiErrorDTO) que GlobalExceptionHandler de ms-inventario. Los errores tecnicos solo se loguean. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Credenciales incorrectas, usuario inexistente o inactivo: mismo mensaje para no revelar cual fue. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorDTO> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("401 login rechazado ({})", ex.getClass().getSimpleName());
        return build(HttpStatus.UNAUTHORIZED.value(), "Credenciales invalidas", request);
    }

    /** Fallo interno al autenticar (ej. base de datos caida): no es culpa del cliente. */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiErrorDTO> handleInternalAuth(InternalAuthenticationServiceException ex,
            HttpServletRequest request) {
        log.error("500 error interno al autenticar", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor", request);
    }

    /** Errores de negocio con codigo propio (409 usuario duplicado, 400 rol inexistente...). El mensaje es apto para el cliente. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApi(ApiException ex, HttpServletRequest request) {
        log.warn("{} {}: {}", ex.getStatus().value(), request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus().value(), ex.getMessage(), request);
    }

    /** Login bloqueado por demasiados intentos fallidos. */
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiErrorDTO> handleTooMany(TooManyAttemptsException ex, HttpServletRequest request) {
        log.warn("429 login bloqueado temporalmente ({} s restantes)", ex.getRetryAfterSeconds());
        ApiErrorDTO body = body(HttpStatus.TOO_MANY_REQUESTS.value(),
                "Demasiados intentos fallidos. Intente nuevamente mas tarde", request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    /** Fallo de @PreAuthorize (ocurre dentro del controller, asi que el filtro de seguridad no lo atrapa). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("403 {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN.value(), "No tiene permisos para acceder a este recurso", request);
    }

    /** Ej. dos altas simultaneas del mismo usuario: la restriccion unica de la base responde antes que el chequeo previo. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("409 integridad {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT.value(), "La operacion viola una restriccion de datos (posible duplicado)", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST.value(), "Datos invalidos. " + detalle, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST.value(), "La solicitud es invalida o esta mal formada", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            log.warn("{} {}: {}", status, request.getRequestURI(), ex.getMessage());
            return build(status, "No se pudo procesar la solicitud", request);
        }
        log.error("500 error inesperado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor", request);
    }

    private ResponseEntity<ApiErrorDTO> build(int status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, message, request));
    }

    private ApiErrorDTO body(int status, String message, HttpServletRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return new ApiErrorDTO()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(resolved != null ? resolved.getReasonPhrase() : "Error")
                .message(message)
                .path(request.getRequestURI());
    }
}
