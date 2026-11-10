package com.ferreteria.apigateway.docs;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega a la interfaz de Swagger la lista de microservicios a documentar, en el formato
 * que espera Swagger UI ({@code {"urls":[{"name":..., "url":...}]}}). Sale de {@code app.swagger.definitions}.
 */
@RestController
public class SwaggerDefinitionsController {

    private final SwaggerProperties properties;

    public SwaggerDefinitionsController(SwaggerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/docs/definitions")
    public Map<String, Object> definitions() {
        return Map.of("urls", properties.getDefinitions());
    }
}
