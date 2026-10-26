package com.ferreteria.ventas.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.commons.dto.DetalleFacturaVentaDTO;
import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.commons.dto.PresupuestoDetalleDTO;
import com.ferreteria.ventas.cache.LoggingCacheManager;

/**
 * Cache en el Redis compartido por todos los microservicios. Claves como texto y valores como JSON.
 * <p>
 * Cada seccion tiene un serializador JSON tipado con su DTO, de modo que al leer se obtiene el DTO correcto sin
 * habilitar "default typing" (que guardaria nombres de clase en el JSON y abre la puerta a deserializar tipos
 * arbitrarios). Cada seccion tiene su propio TTL (ver {@link CacheProperties}). Todas las operaciones sobre la cache
 * quedan en el log gracias al decorador {@link LoggingCacheManager}.
 * <p>
 * Redis es OPCIONAL: el {@code CacheManager} que usa la aplicacion es {@link ResilientCacheManager}, que ante cualquier
 * fallo de Redis deja pasar la operacion a la base de datos (ver esa clase).
 * <p>
 * Las anotaciones (@EnableCaching) se activan en {@code VentasApplication}.
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties props,
            @Value("${app.cache.retry-seconds:30}") long retrySeconds) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .prefixCacheNameWith(props.keyPrefix())
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8));

        Map<String, RedisCacheConfiguration> perCache = new LinkedHashMap<>();
        perCache.put(CacheNames.CLIENTES, section(base, props, CacheNames.CLIENTES, ClienteDTO.class));
        perCache.put(CacheNames.PRESUPUESTOS, section(base, props, CacheNames.PRESUPUESTOS, PresupuestoDTO.class));
        perCache.put(CacheNames.PRESUPUESTOS_DETALLE,
                section(base, props, CacheNames.PRESUPUESTOS_DETALLE, PresupuestoDetalleDTO.class));
        perCache.put(CacheNames.FACTURAS_VENTAS,
                section(base, props, CacheNames.FACTURAS_VENTAS, FacturaVentaDTO.class));
        perCache.put(CacheNames.DETALLES_FACTURAS_VENTAS,
                section(base, props, CacheNames.DETALLES_FACTURAS_VENTAS, DetalleFacturaVentaDTO.class));

        RedisCacheManager redis = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(Math.max(props.defaultTtlMinutes(), 0))))
                .withInitialCacheConfigurations(perCache)
                // solo existen las secciones declaradas arriba (un nombre mal escrito falla al usarse)
                .disableCreateOnMissingCache()
                .build();
        // el manager interno no es un bean (el bean es el decorador), asi que Spring no lo inicializa solo
        redis.afterPropertiesSet();
        return new ResilientCacheManager(new LoggingCacheManager(redis), Duration.ofSeconds(retrySeconds));
    }

    private static <T> RedisCacheConfiguration section(RedisCacheConfiguration base, CacheProperties props,
            String cacheName, Class<T> dtoType) {
        return base.entryTtl(props.ttlFor(cacheName))
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(dtoType)));
    }
}
