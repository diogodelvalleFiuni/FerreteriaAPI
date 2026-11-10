package com.ferreteria.seguridad.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param maxAttempts intentos fallidos (misma IP + mismo usuario) antes de bloquear
 * @param lockMinutes minutos de bloqueo; tambien es la ventana en la que se cuentan los fallos
 */
@ConfigurationProperties(prefix = "app.security.login-limit")
public record LoginLimitProperties(
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("15") long lockMinutes) {
}
