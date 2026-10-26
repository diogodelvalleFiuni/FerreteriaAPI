package com.ferreteria.compras.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recursoInexistenteDevuelve404ConApiErrorDTO() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("no existe"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    void errorInesperadoDevuelve500SinExponerDetallesTecnicos() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }

    @Test
    void validacionFallidaDevuelve400() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void reglaDeNegocioDevuelve400ConElMensaje() throws Exception {
        mockMvc.perform(get("/test/regla"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("regla violada"));
    }

    @Test
    void recursoDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(get("/test/duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("ya existe"));
    }

    @Test
    void metodoNoPermitidoConservaElCodigo405() throws Exception {
        mockMvc.perform(post("/test/boom"))
                .andExpect(status().isMethodNotAllowed());
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("no existe");
        }

        @GetMapping("/test/regla")
        String regla() {
            throw new BusinessRuleException("regla violada");
        }

        @GetMapping("/test/duplicado")
        String duplicado() {
            throw new DuplicateResourceException("ya existe");
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("detalle tecnico que no debe salir");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody Body body) {
            return "ok";
        }
    }

    record Body(@NotBlank String nombre) {
    }
}
