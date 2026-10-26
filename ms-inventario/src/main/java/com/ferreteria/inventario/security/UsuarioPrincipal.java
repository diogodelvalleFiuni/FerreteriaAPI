package com.ferreteria.inventario.security;

import java.security.Principal;
import java.util.List;

/**
 * Identidad del usuario autenticado, armada con los datos del token validado.
 * Se obtiene en los controllers con {@code @AuthenticationPrincipal UsuarioPrincipal} o
 * {@code SecurityContextHolder.getContext().getAuthentication().getPrincipal()}.
 *
 * @param username claim "sub" del token
 * @param roles    roles del token, tal cual vienen (sin prefijo ROLE_)
 */
public record UsuarioPrincipal(String username, List<String> roles) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
