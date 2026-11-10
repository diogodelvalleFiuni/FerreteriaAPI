package com.ferreteria.inventario.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.ferreteria.commons.dto.CategoriaProductoDTO;
import com.ferreteria.commons.dto.DepositoDTO;
import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.commons.dto.MarcaDTO;
import com.ferreteria.commons.dto.ProductoDTO;
import com.ferreteria.commons.dto.TransferenciaDTO;

/**
 * Cache en Redis. Claves como texto y valores como JSON.
 * <p>
 * Cada seccion tiene un serializador JSON tipado con su DTO, de modo que al leer se obtiene
 * el DTO correcto sin habilitar "default typing" (que guardaria nombres de clase en el JSON
 * y abre la puerta a deserializar tipos arbitrarios).
 * <p>
 * Redis es opcional: el {@code CacheManager} que usa la aplicacion es {@link ResilientCacheManager}, que ante cualquier
 * fallo de Redis deja pasar la operacion a la base de datos (ver esa clase).
 * <p>
 * Las anotaciones (@EnableCaching) se activan en {@code InventarioApplication}.
 */
@Configuration
public class CacheConfig {

    /** El que usa la aplicacion: Redis envuelto para que su ausencia no sea un impedimento. */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisCacheManager redisCacheManager,
            @Value("${app.cache.retry-seconds:30}") long retrySeconds) {
        return new ResilientCacheManager(redisCacheManager, Duration.ofSeconds(retrySeconds));
    }

    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${app.cache.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.cache.key-prefix:inventario::}") String keyPrefix) {

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .disableCachingNullValues()
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8));

        Map<String, RedisCacheConfiguration> perCache = new LinkedHashMap<>();
        perCache.put(CacheNames.CATEGORIAS, jsonFor(base, CategoriaProductoDTO.class));
        perCache.put(CacheNames.DEPOSITOS, jsonFor(base, DepositoDTO.class));
        perCache.put(CacheNames.DETALLES_TRANSFERENCIA, jsonFor(base, DetalleTransferenciaDTO.class));
        perCache.put(CacheNames.INVENTARIOS, jsonFor(base, InventarioDTO.class));
        perCache.put(CacheNames.MARCAS, jsonFor(base, MarcaDTO.class));
        perCache.put(CacheNames.PRODUCTOS, jsonFor(base, ProductoDTO.class));
        perCache.put(CacheNames.TRANSFERENCIAS, jsonFor(base, TransferenciaDTO.class));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                // solo existen las secciones declaradas arriba (un nombre mal escrito falla al usarse)
                .disableCreateOnMissingCache()
                .build();
    }

    private static <T> RedisCacheConfiguration jsonFor(RedisCacheConfiguration base, Class<T> dtoType) {
        return base.serializeValuesWith(
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(dtoType)));
    }
}
