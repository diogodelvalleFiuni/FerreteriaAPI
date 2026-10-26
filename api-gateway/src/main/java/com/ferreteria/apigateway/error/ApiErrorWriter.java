package com.ferreteria.apigateway.error;

import java.io.IOException;
import java.time.OffsetDateTime;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Escribe un {@link ApiError} como JSON en la respuesta. Nunca incluye detalles tecnicos. */
@Component
public class ApiErrorWriter {

    private final ObjectMapper mapper;

    public ApiErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse res, int status, String path) throws IOException {
        HttpStatus resolved = HttpStatus.resolve(status);
        ApiError body = new ApiError(
                OffsetDateTime.now().toString(),
                status,
                resolved != null ? resolved.getReasonPhrase() : "Error",
                messageFor(status),
                path);
        res.resetBuffer();
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(mapper.writeValueAsString(body));
    }

    /** Mensaje generico por codigo: no se expone el mensaje de la excepcion original. */
    public static String messageFor(int status) {
        return switch (status) {
            case 400 -> "La solicitud es invalida o esta mal formada";
            case 401 -> "Credenciales invalidas o ausentes";
            case 403 -> "No tiene permiso para esta operacion";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Metodo no permitido";
            case 502 -> "No se pudo comunicar con el servicio";
            case 503 -> "Servicio no disponible temporalmente";
            case 504 -> "El servicio no respondio a tiempo";
            default -> status >= 500 ? "Error interno del servidor" : "No se pudo procesar la solicitud";
        };
    }
}
