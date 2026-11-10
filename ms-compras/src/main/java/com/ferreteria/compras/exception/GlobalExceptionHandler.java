package com.ferreteria.compras.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ferreteria.commons.dto.ApiErrorDTO;

/**
 * Manejo centralizado de errores. Todas las respuestas usan el {@link ApiErrorDTO}
 * comun de commons. Los errores tecnicos se loguean pero nunca se exponen al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("404 {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND.value(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorDTO> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("400 regla de negocio {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorDTO> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("409 duplicado {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT.value(), ex.getMessage(), request);
    }

    /** Fallo de @PreAuthorize (ocurre dentro del controller, asi que el filtro de seguridad no lo atrapa): 403, no 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("403 {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN.value(), "No tiene permisos para acceder a este recurso", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("400 validacion {}: {}", request.getRequestURI(), detalle);
        return build(HttpStatus.BAD_REQUEST.value(), "Datos invalidos. " + detalle, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorDTO> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("400 {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST.value(), "La solicitud es invalida o esta mal formada", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("409 integridad {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT.value(),
                "La operacion viola una restriccion de datos (referencia inexistente, duplicado o registro en uso)", request);
    }

    /** Cualquier otra excepcion: los errores del framework conservan su codigo (405, 415...), el resto es 500. */
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
        HttpStatus resolved = HttpStatus.resolve(status);
        ApiErrorDTO body = new ApiErrorDTO()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(resolved != null ? resolved.getReasonPhrase() : "Error")
                .message(message)
                .path(request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
