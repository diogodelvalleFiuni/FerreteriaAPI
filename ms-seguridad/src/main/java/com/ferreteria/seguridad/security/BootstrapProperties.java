package com.ferreteria.seguridad.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Credenciales del PRIMER administrador. Solo se usan si la tabla de usuarios esta vacia; despues de la
 * primera ejecucion conviene quitar las variables. No hay valores por defecto: sin variables no se crea nada.
 */
@ConfigurationProperties(prefix = "app.security.bootstrap")
public record BootstrapProperties(
        @DefaultValue("") String adminUsername,
        @DefaultValue("") String adminPassword) {

    public boolean configured() {
        return !adminUsername.isBlank() && !adminPassword.isBlank();
    }

    /** Evita que la clave aparezca si el objeto se loguea por error. */
    @Override
    public String toString() {
        return "BootstrapProperties[adminUsername=" + adminUsername + ", adminPassword=****]";
    }
}
