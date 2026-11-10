package com.ferreteria.ventas.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Reglas por rol de la seguridad a nivel de metodo (@PreAuthorize -> RoleAccess). */
class RoleAccessTest {

    private final RoleAccess access = new RoleAccess(new AccessProperties(
            List.of("ADMIN", "VENDEDOR"), List.of("ADMIN", "VENDEDOR"), List.of("ADMIN")));

    private static Authentication userWith(String... roles) {
        return new UsernamePasswordAuthenticationToken("juan", "n/a",
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }

    @Test
    void adminPuedeLeerEscribirYBorrar() {
        Authentication admin = userWith("ADMIN");
        assertTrue(access.canRead(admin));
        assertTrue(access.canWrite(admin));
        assertTrue(access.canDelete(admin));
    }

    @Test
    void vendedorLeeYEscribePeroNoBorra() {
        Authentication vendedor = userWith("VENDEDOR");
        assertTrue(access.canRead(vendedor));
        assertTrue(access.canWrite(vendedor));
        assertFalse(access.canDelete(vendedor));
    }

    @Test
    void conVariosRolesAlcanzaConUnoAutorizado() {
        Authentication multiple = userWith("COMPRADOR", "VENDEDOR");
        assertTrue(access.canRead(multiple));
        assertFalse(access.canDelete(multiple));
    }

    @Test
    void unRolAjenoNoAccedeANada() {
        Authentication comprador = userWith("COMPRADOR");
        assertFalse(access.canRead(comprador));
        assertFalse(access.canWrite(comprador));
        assertFalse(access.canDelete(comprador));
    }

    @Test
    void sinAutenticacionSeNiegaTodo() {
        assertFalse(access.canRead(null));
    }

    @Test
    void losRolesConfiguradosTambienPuedenVenirConPrefijoRole() {
        RoleAccess conPrefijo = new RoleAccess(new AccessProperties(
                List.of("ROLE_ADMIN"), List.of("ROLE_ADMIN"), List.of("ROLE_ADMIN")));
        assertTrue(conPrefijo.canRead(userWith("ADMIN")));
    }
}
