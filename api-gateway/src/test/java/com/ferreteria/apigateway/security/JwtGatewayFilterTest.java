package com.ferreteria.apigateway.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferreteria.apigateway.error.ApiErrorWriter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

class JwtGatewayFilterTest {

    private static final String SECRET = "un-secret-de-prueba-bastante-largo-para-hs256-0123456789";

    private final JwtGatewayFilter filter =
            new JwtGatewayFilter(new JwtService(SECRET), new ApiErrorWriter(new ObjectMapper()),
                    List.of("/auth/", "/actuator/health"));

    private String token(String secret, long expiraEnMs) {
        return Jwts.builder()
                .setSubject("usuario")
                .setExpiration(new Date(System.currentTimeMillis() + expiraEnMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private MockHttpServletResponse call(String uri, String authorization, MockFilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRequestURI(uri);
        if (authorization != null) {
            req.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse res = call("/api/depositos", null, chain);

        assertEquals(401, res.getStatus());
        assertNull(chain.getRequest());
        String body = res.getContentAsString();
        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("\"error\":\"Unauthorized\""));
        assertTrue(body.contains("\"path\":\"/api/depositos\""));
    }

    @Test
    void tokenValidoPasaAlRuteo() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse res = call("/api/depositos", "Bearer " + token(SECRET, 60_000), chain);

        assertEquals(200, res.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void tokenFirmadoConOtroSecretDevuelve401() throws Exception {
        String otro = "otro-secret-distinto-igual-de-largo-para-hs256-9876543210";
        MockHttpServletResponse res = call("/api/depositos", "Bearer " + token(otro, 60_000), new MockFilterChain());

        assertEquals(401, res.getStatus());
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        MockHttpServletResponse res = call("/api/depositos", "Bearer " + token(SECRET, -60_000), new MockFilterChain());

        assertEquals(401, res.getStatus());
    }

    @Test
    void rutaPublicaNoExigeToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse res = call("/auth/login", null, chain);

        assertEquals(200, res.getStatus());
        assertNotNull(chain.getRequest());
    }
}
