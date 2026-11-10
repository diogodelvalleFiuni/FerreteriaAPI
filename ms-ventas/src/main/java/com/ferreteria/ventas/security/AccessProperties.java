package com.ferreteria.ventas.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Roles autorizados por tipo de operacion (prefijo {@code app.security.roles}). Un usuario puede tener varios roles:
 * alcanza con que tenga UNO de los listados. Los nombres van sin el prefijo ROLE_ y deben coincidir con los de la
 * tabla {@code roles} de ms-seguridad (claim "roles" del token).
 * <ul>
 *     <li>{@code read}   - GET y las busquedas (POST .../search)</li>
 *     <li>{@code write}  - POST (crear) y PUT (actualizar)</li>
 *     <li>{@code delete} - DELETE</li>
 * </ul>
 * La misma configuracion alimenta la regla por URL ({@link SecurityConfig}) y la regla por metodo
 * ({@code @PreAuthorize} -> {@link RoleAccess}), asi las dos capas nunca se contradicen.
 */
@ConfigurationProperties(prefix = "app.security.roles")
public record AccessProperties(
        @DefaultValue({"ADMIN", "VENDEDOR"}) List<String> read,
        @DefaultValue({"ADMIN", "VENDEDOR"}) List<String> write,
        @DefaultValue({"ADMIN"}) List<String> delete) {

    public String[] readRoles() {
        return read.toArray(String[]::new);
    }

    public String[] writeRoles() {
        return write.toArray(String[]::new);
    }

    public String[] deleteRoles() {
        return delete.toArray(String[]::new);
    }
}
