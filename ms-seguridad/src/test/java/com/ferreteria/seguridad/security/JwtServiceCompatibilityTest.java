package com.ferreteria.seguridad.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** El access token emitido aca debe poder ser leido por el JwtDecoder de ms-inventario; el refresh token NO. */
class JwtServiceCompatibilityTest {

    private static final String SECRET = "clave-compartida-de-prueba-32-bytes-minimo!";
    private static final String REFRESH_SECRET = "clave-de-refresh-de-prueba-32-bytes-minimo!!";

    private final JwtProperties props =
            new JwtProperties(SECRET, false, "HS256", "", "roles", 60, REFRESH_SECRET, 7);
    private final JwtConfig config = new JwtConfig();
    private final JwtService service = new JwtService(config.jwtEncoder(props), config.refreshJwtEncoder(props),
            config.refreshJwtDecoder(props), props);

    /** Decoder construido igual que ms-inventario/JwtConfig (HS256, validador por defecto, SOLO la clave de access). */
    private Jwt decodeLikeInventario(String token) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefault());
        return decoder.decode(token);
    }

    @Test
    void tokenTiene3SeccionesYLoAceptaElDecoderDeInventario() {
        String token = service.generateToken("juan",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("VENDEDOR")));

        assertEquals(3, token.split("\\.", -1).length);
        Jwt jwt = decodeLikeInventario(token);
        assertEquals("juan", jwt.getSubject());
        assertEquals(List.of("ADMIN", "VENDEDOR"), jwt.getClaimAsStringList("roles"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }

    @Test
    void claimsSoloContienenUsernameRolesYFechas() {
        Jwt jwt = decodeLikeInventario(service.generateToken("ana", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertEquals(Set.of("sub", "roles", "iat", "exp"), jwt.getClaims().keySet());
        assertNull(jwt.getClaim("id_persona"));
    }

    @Test
    void refreshTokenSeValidaYDevuelveElUsuario() {
        assertEquals("juan", service.validateRefreshToken(service.generateRefreshToken("juan")));
    }

    @Test
    void refreshTokenNoTieneRolesYTraeTokenUseYJti() {
        String refresh = service.generateRefreshToken("juan");
        Jwt jwt = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(REFRESH_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build().decode(refresh);
        assertNull(jwt.getClaim("roles"));
        assertEquals("refresh", jwt.getClaimAsString("token_use"));
        assertNotNull(jwt.getId());
    }

    @Test
    void unRefreshTokenNoLoAceptaElDecoderDeInventario() {
        String refresh = service.generateRefreshToken("juan");
        assertThrows(JwtException.class, () -> decodeLikeInventario(refresh));
    }

    @Test
    void unAccessTokenNoSirveComoRefreshToken() {
        String access = service.generateToken("juan", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThrows(JwtException.class, () -> service.validateRefreshToken(access));
    }

    @Test
    void basuraNoSirveComoRefreshToken() {
        assertThrows(JwtException.class, () -> service.validateRefreshToken("a.b.c"));
    }

    @Test
    void laClaveDeRefreshNoPuedeSerIgualALaDeAccess() {
        JwtProperties iguales = new JwtProperties(SECRET, false, "HS256", "", "roles", 60, SECRET, 7);
        assertThrows(IllegalStateException.class, () -> new JwtConfig().refreshJwtEncoder(iguales));
    }

    @Test
    void laClaveDeRefreshEsObligatoriaYDebeSerLarga() {
        JwtProperties sinClave = new JwtProperties(SECRET, false, "HS256", "", "roles", 60, "", 7);
        JwtProperties corta = new JwtProperties(SECRET, false, "HS256", "", "roles", 60, "corta", 7);
        assertThrows(IllegalStateException.class, () -> new JwtConfig().refreshJwtDecoder(sinClave));
        assertThrows(IllegalStateException.class, () -> new JwtConfig().refreshJwtDecoder(corta));
    }
}
