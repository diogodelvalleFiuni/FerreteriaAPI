package com.ferreteria.compras.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tools.jackson.databind.json.JsonMapper;

/** Prueba el filtro con un token real firmado HS256, sin levantar el contexto de Spring. */
class AuthFilterTest {

    private static final String SECRET = "clave-compartida-de-prueba-32-bytes-minimo!";

    private AuthFilter filter;
    private NimbusJwtEncoder encoder;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, false, "HS256", "", "roles");
        filter = new AuthFilter(new JwtConfig().jwtDecoder(props), props,
                new RestSecurityErrorHandler(JsonMapper.builder().build()));
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        SecurityContextHolder.clearContext();
    }

    private String token(Instant expiresAt, Object roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("juan")
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private MockHttpServletRequest requestWith(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/productos");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    void tokenValidoPublicaElPrincipalEnElContexto() throws Exception {
        AtomicReference<Authentication> seen = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                seen.set(SecurityContextHolder.getContext().getAuthentication());
                super.doFilter(req, res);
            }
        };

        filter.doFilter(requestWith("Bearer " + token(Instant.now().plusSeconds(600), List.of("ADMIN"))),
                response, chain);

        Authentication auth = seen.get();
        assertNotNull(auth);
        UsuarioPrincipal principal = (UsuarioPrincipal) auth.getPrincipal();
        assertEquals("juan", principal.getName());
        assertEquals(List.of("ADMIN"), principal.roles());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "el contexto se limpia al terminar");
    }

    @Test
    void sinHeaderSigueLaCadenaSinAutenticar() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith(null), response, chain);

        assertNotNull(chain.getRequest(), "la peticion siguio la cadena");
        assertEquals(200, response.getStatus());
    }

    @Test
    void tokenConFirmaDistintaDevuelve401() throws Exception {
        String valid = token(Instant.now().plusSeconds(600), List.of("ADMIN"));
        String tampered = valid.substring(0, valid.length() - 3) + (valid.endsWith("AAA") ? "BBB" : "AAA");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith("Bearer " + tampered), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requestWith("Bearer " + token(Instant.now().minusSeconds(120), List.of())),
                response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void tokenSinTresSeccionesDevuelve401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWith("Bearer solo.dos"), response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void formatoDeTresSecciones() {
        assertTrue(JwtSpec.hasValidFormat("a.b.c"));
        assertFalse(JwtSpec.hasValidFormat("a.b"));
        assertFalse(JwtSpec.hasValidFormat("a.b.c.d"));
        assertFalse(JwtSpec.hasValidFormat("a..c"));
        assertFalse(JwtSpec.hasValidFormat(null));
    }

    @Test
    void claveCortaHaceFallarElArranque() {
        JwtProperties corta = new JwtProperties("corta", false, "HS256", "", "roles");
        assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtDecoder(corta));
    }
}
