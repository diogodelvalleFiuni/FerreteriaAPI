package com.ferreteria.compras.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Metadatos de la documentacion OpenAPI/Swagger. Todo configurable desde application.properties (app.openapi.*).
 * UI: /swagger-ui.html - JSON: /v3/api-docs (rutas configurables en springdoc.*).
 * <p>
 * {@code app.openapi.server-url}: "/" (por defecto) hace que "Try it out" llame al mismo host desde el que se abrio
 * el Swagger. Asi, cuando el JSON se carga desde el Swagger del API Gateway, las pruebas pasan por el gateway.
 * Tambien declara el esquema Bearer (JWT) para poder usar el boton Authorize.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI comprasOpenApi(
            @Value("${app.openapi.title}") String title,
            @Value("${app.openapi.description}") String description,
            @Value("${app.openapi.version}") String version,
            @Value("${app.openapi.server-url}") String serverUrl) {
        return new OpenAPI()
                .info(new Info().title(title).description(description).version(version))
                .servers(List.of(new Server().url(serverUrl)))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
