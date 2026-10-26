package com.ferreteria.ventas.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Politica de retencion de la cache (prefijo {@code app.cache}).
 * <ul>
 *     <li>{@code key-prefix} - prefijo de todas las claves de este servicio en el Redis compartido</li>
 *     <li>{@code default-ttl-minutes} - TTL para una seccion que no tenga el suyo</li>
 *     <li>{@code ttl-minutes.<seccion>} - TTL por seccion, en minutos. <b>0 o menos = persistente</b> (no expira;
 *         solo se invalida al actualizar o borrar el objeto)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        @DefaultValue("ventas::") String keyPrefix,
        @DefaultValue("10") long defaultTtlMinutes,
        @DefaultValue Map<String, Long> ttlMinutes) {

    /** TTL de la seccion; {@link Duration#ZERO} significa que no expira. */
    public Duration ttlFor(String cacheName) {
        long minutes = ttlMinutes.getOrDefault(cacheName, defaultTtlMinutes);
        return minutes <= 0 ? Duration.ZERO : Duration.ofMinutes(minutes);
    }
}
