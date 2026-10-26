package com.ferreteria.compras.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.commons.dto.ProveedorDTO;

/**
 * Cache en Redis. Claves como texto ({@code compras::<seccion>::<id>}) y valores como JSON.
 * <p>
 * Cada seccion tiene un serializador JSON tipado con su DTO, de modo que al leer se obtiene el DTO correcto sin
 * habilitar "default typing" (que guardaria nombres de clase en el JSON y abre la puerta a deserializar tipos
 * arbitrarios). No se guardan nulos, y el TTL / prefijo / tiempo de reintento salen de application.properties.
 * <p>
 * Capas del {@code CacheManager} expuesto (de afuera hacia adentro):
 * <ol>
 *     <li>{@link ResilientCacheManager}: Redis es OPCIONAL. Ante cualquier fallo de Redis la operacion se trata como
 *         "no esta en la cache" y el servicio sigue con la base de datos, sin lanzar error (ver esa clase).</li>
 *     <li>{@link LoggingCacheManager}: trazabilidad, registra cada get / put / evict / clear.</li>
 *     <li>{@link RedisCacheManager}: el almacen real.</li>
 * </ol>
 * Las anotaciones (@EnableCaching) se activan en {@code ComprasApplication}.
 */
@Configuration
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.ttl-minutes}") long ttlMinutes,
            @Value("${app.cache.key-prefix}") String keyPrefix,
            @Value("${app.cache.log-level:INFO}") String logLevel,
            @Value("${app.cache.retry-seconds:30}") long retrySeconds) {

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .disableCachingNullValues()
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8));

        Map<String, RedisCacheConfiguration> perCache = new LinkedHashMap<>();
        perCache.put(CacheNames.PROVEEDORES, jsonFor(base, ProveedorDTO.class));
        perCache.put(CacheNames.FACTURAS_COMPRA, jsonFor(base, FacturaCompraDTO.class));
        perCache.put(CacheNames.DETALLES_FACTURA_COMPRA, jsonFor(base, DetalleFacturaCompraDTO.class));

        RedisCacheManager redis = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                // put/evict se aplican DESPUES del commit: otro hilo no vuelve a cachear un valor viejo
                .transactionAware()
                // solo existen las secciones declaradas arriba (un nombre mal escrito falla al usarse)
                .disableCreateOnMissingCache()
                .build();
        redis.afterPropertiesSet();
        log.info("Cache Redis: prefijo '{}', TTL {} min, secciones {}", keyPrefix, ttlMinutes, perCache.keySet());
        return new ResilientCacheManager(new LoggingCacheManager(redis, parseLevel(logLevel)),
                Duration.ofSeconds(retrySeconds));
    }

    private static <T> RedisCacheConfiguration jsonFor(RedisCacheConfiguration base, Class<T> dtoType) {
        return base.serializeValuesWith(
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(dtoType)));
    }

    private static Level parseLevel(String name) {
        try {
            return Level.valueOf(name.trim().toUpperCase());
        } catch (RuntimeException ex) {
            return Level.INFO;
        }
    }
}
