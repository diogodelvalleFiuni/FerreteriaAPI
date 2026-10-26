package com.ferreteria.inventario.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.RedisConnectionFailureException;

class ResilientCacheManagerTest {

    private final Cache redis = mock(Cache.class);
    private final CacheManager delegate = mock(CacheManager.class);

    private Cache resilient() {
        when(delegate.getCache("productos")).thenReturn(redis);
        return new ResilientCacheManager(delegate, Duration.ofSeconds(60)).getCache("productos");
    }

    @Test
    void conRedisFuncionandoDelegaNormalmente() {
        when(redis.get(1L)).thenReturn(new SimpleValueWrapper("dto"));

        assertEquals("dto", resilient().get(1L).get());
    }

    @Test
    void redisCaidoEnLecturaEsUnMissYNoUnError() {
        when(redis.get(1L)).thenThrow(new RedisConnectionFailureException("sin redis"));

        assertNull(resilient().get(1L)); // @Cacheable ejecuta el metodo = lee la base
    }

    @Test
    void redisCaidoEnEscrituraEInvalidacionNoLanzaError() {
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("sin redis")).when(redis).put(1L, "dto");
        Cache cache = resilient();

        cache.put(1L, "dto");      // @CachePut
        cache.evict(1L);           // @CacheEvict (ya con el corte abierto)
        assertNotNull(cache);
    }

    @Test
    void trasUnFalloNoSeVuelveAConsultarRedisDuranteElCorte() {
        when(redis.get(1L)).thenThrow(new RedisConnectionFailureException("sin redis"));
        Cache cache = resilient();

        cache.get(1L);
        cache.get(2L);
        cache.put(3L, "x");

        verify(redis, times(1)).get(1L);
        verify(redis, times(0)).get(2L);
        verify(redis, times(0)).put(3L, "x");
    }
}
