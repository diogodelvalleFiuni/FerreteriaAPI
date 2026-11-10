package com.ferreteria.seguridad.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import tools.jackson.databind.json.JsonMapper;

/** El filtro de ms-seguridad valida el access token Y vuelve a leer al usuario en la base. */
class AuthFilterTest {

    private static final String SECRET = "clave-compartida-de-prueba-32-bytes-minimo!";
    private static final String REFRESH_SECRET = "clave-de-refresh-de-prueba-32-bytes-minimo!!";

    private final JwtProperties props =
            new JwtProperties(SECRET, false, "HS256", "", "roles", 60, REFRESH_SECRET, 7);
    private final JwtConfig config = new JwtConfig();
    private final JwtService jwtService = new JwtService(config.jwtEncoder(props), config.refreshJwtEncoder(props),
            config.refreshJwtDecoder(props), props);
    private final UserDetailsService users = mock(UserDetailsService.class);
    private AuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthFilter(config.jwtDecoder(props), users, new RestSecurityErrorHandler(JsonMapper.builder().build()));
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String method, String uri, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    private String accessToken() {
        return jwtService.generateToken("juan", List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR")));
    }

    @Test
    void tokenValidoYUsuarioActivoAutenticaConLosRolesDeLaBase() throws Exception {
        when(users.loadUserByUsername("juan")).thenReturn(
                User.withUsername("juan").password("x").authorities("ROLE_ADMIN").build());
        AtomicReference<Authentication> seen = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                seen.set(SecurityContextHolder.getContext().getAuthentication());
                super.doFilter(req, res);
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/auth/me", "Bearer " + accessToken()), response, chain);

        assertNotNull(chain.getRequest());
        Authentication auth = seen.get();
        assertNotNull(auth);
        assertEquals("juan", auth.getName());
        // el token decia VENDEDOR, pero en la base ahora es ADMIN: manda la base
        assertEquals(List.of("ROLE_ADMIN"), auth.getAuthorities().stream().map(Object::toString).toList());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void usuarioDeshabilitadoEnLaBaseRecibe401() throws Exception {
        when(users.loadUserByUsername("juan")).thenReturn(
                User.withUsername("juan").password("x").authorities("ROLE_ADMIN").disabled(true).build());
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/auth/me", "Bearer " + accessToken()), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void usuarioQueYaNoExisteRecibe401() throws Exception {
        when(users.loadUserByUsername("juan")).thenThrow(new UsernameNotFoundException("no"));
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/auth/me", "Bearer " + accessToken()), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void unRefreshTokenNoAutenticaRutasProtegidas() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/auth/me", "Bearer " + jwtService.generateRefreshToken("juan")), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void headerSinBearerOTokenMalformadoRecibe401() throws Exception {
        for (String header : List.of("Basic abc", "Bearer abc.def")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/api/auth/me", header), response, chain);
            assertEquals(401, response.getStatus());
            assertNull(chain.getRequest());
        }
    }

    @Test
    void sinHeaderPasaSinAutenticarYLaCadenaDecide() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/auth/me", null), response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void loginYRefreshNoPasanPorElFiltroAunqueTraigaUnTokenVencido() throws Exception {
        for (String uri : List.of("/api/auth/login", "/api/auth/refresh")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("POST", uri, "Bearer aaa.bbb.ccc"), response, chain);
            assertTrue(response.getStatus() != 401, uri);
            assertNotNull(chain.getRequest(), uri);
        }
    }
}
