package com.ferreteria.apigateway.error;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Los errores que resuelve Spring por su cuenta (ruta inexistente, metodo no permitido...) pasan por
 * /error. Este bean les da el mismo formato que {@link ApiError}.
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> base = super.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
        int status = base.get("status") instanceof Integer i ? i : 500;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status);
        body.put("error", base.get("error"));
        body.put("message", ApiErrorWriter.messageFor(status));
        body.put("path", base.get("path"));
        return body;
    }
}
