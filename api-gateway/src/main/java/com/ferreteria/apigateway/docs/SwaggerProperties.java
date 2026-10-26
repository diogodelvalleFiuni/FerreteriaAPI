package com.ferreteria.apigateway.docs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Definiciones OpenAPI que muestra la interfaz de Swagger comun ({@code app.swagger.definitions}). */
@Component
@ConfigurationProperties(prefix = "app.swagger")
public class SwaggerProperties {

    private List<Definition> definitions = new ArrayList<>();

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<Definition> definitions) {
        this.definitions = definitions;
    }

    /** Un microservicio documentado: nombre visible y URL (relativa a /swagger-ui.html) de su OpenAPI. */
    public static class Definition {

        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
