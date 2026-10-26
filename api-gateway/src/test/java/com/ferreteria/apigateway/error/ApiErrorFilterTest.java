package com.ferreteria.apigateway.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.ObjectMapper;

class ApiErrorFilterTest {

    private final ApiErrorFilter filter = new ApiErrorFilter(new ApiErrorWriter(new ObjectMapper()));

    private MockHttpServletResponse run(FilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/depositos");
        req.setRequestURI("/api/depositos");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    @Test
    void servicioSinInstanciasDevuelve503SinFiltrarElNombreInterno() throws Exception {
        MockHttpServletResponse res = run((rq, rs) -> {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to find instance for ms-inventario");
        });

        String body = res.getContentAsString();
        assertEquals(503, res.getStatus());
        assertTrue(body.contains("\"status\":503"));
        assertTrue(body.contains("Servicio no disponible temporalmente"));
        assertFalse(body.contains("ms-inventario"));
    }

    @Test
    void servicioCaidoDevuelve502() throws Exception {
        MockHttpServletResponse res = run((rq, rs) -> {
            throw new ResourceAccessException("I/O error", new ConnectException("Connection refused"));
        });

        assertEquals(502, res.getStatus());
        assertTrue(res.getContentAsString().contains("No se pudo comunicar con el servicio"));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesTecnicos() throws Exception {
        MockHttpServletResponse res = run((rq, rs) -> {
            throw new IllegalStateException("detalle tecnico que no debe salir");
        });

        assertEquals(500, res.getStatus());
        assertTrue(res.getContentAsString().contains("Error interno del servidor"));
        assertFalse(res.getContentAsString().contains("detalle tecnico"));
    }
}
