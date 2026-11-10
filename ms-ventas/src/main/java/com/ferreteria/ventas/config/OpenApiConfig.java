package com.ferreteria.ventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Metadatos de la documentacion OpenAPI/Swagger. Todo configurable desde application.yml (app.openapi.*).
 * UI: /swagger-ui.html - JSON: /v3/api-docs (rutas configurables en springdoc.*).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ventasOpenApi(
            @Value("${app.openapi.title}") String title,
            @Value("${app.openapi.description}") String description,
            @Value("${app.openapi.version}") String version) {
        // boton "Authorize" en Swagger UI para pegar el token Bearer (emitido por ms-seguridad)
        return new OpenAPI()
                .info(new Info().title(title).description(description).version(version))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
