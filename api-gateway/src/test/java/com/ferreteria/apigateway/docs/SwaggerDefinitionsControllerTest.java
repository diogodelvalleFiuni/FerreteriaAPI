package com.ferreteria.apigateway.docs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SwaggerDefinitionsControllerTest {

    private static SwaggerProperties.Definition definition(String name, String url) {
        SwaggerProperties.Definition d = new SwaggerProperties.Definition();
        d.setName(name);
        d.setUrl(url);
        return d;
    }

    @Test
    void devuelveLasDefinicionesConfiguradasEnElFormatoDeSwaggerUi() throws Exception {
        SwaggerProperties props = new SwaggerProperties();
        props.setDefinitions(List.of(definition("Inventario", "docs/inventario"), definition("Ventas", "docs/ventas")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SwaggerDefinitionsController(props)).build();

        mockMvc.perform(get("/docs/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[0].name").value("Inventario"))
                .andExpect(jsonPath("$.urls[0].url").value("docs/inventario"))
                .andExpect(jsonPath("$.urls[1].name").value("Ventas"))
                .andExpect(jsonPath("$.urls[1].url").value("docs/ventas"));
    }
}
