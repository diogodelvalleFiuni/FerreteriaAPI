package com.ferreteria.seguridad.security;

import java.security.Principal;
import java.util.List;

/** Identidad del usuario autenticado (roles sin prefijo ROLE_), igual que UsuarioPrincipal de ms-inventario. */
public record UsuarioPrincipal(String username, List<String> roles) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
