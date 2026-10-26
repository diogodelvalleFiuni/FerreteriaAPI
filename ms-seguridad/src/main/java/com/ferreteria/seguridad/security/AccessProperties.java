package com.ferreteria.seguridad.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param activeEstadoId id_estado (tabla estados) que permite iniciar sesion. */
@ConfigurationProperties(prefix = "app.security.access")
public record AccessProperties(@DefaultValue("1") Long activeEstadoId) {
}
