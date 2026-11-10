package com.ferreteria.ventas.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/** El decorador de logging no debe alterar el comportamiento de la cache real. */
class LoggingCacheTest {

    private final ConcurrentMapCache real = new ConcurrentMapCache("clientes");
    private final LoggingCache cache = new LoggingCache(real);

    @Test
    void putGetYEvictDelegaEnLaCacheReal() {
        cache.put(1L, "a");

        assertEquals("a", cache.get(1L).get());
        assertNull(cache.get(2L));

        cache.evict(1L);
        assertNull(real.get(1L));
    }

    @Test
    void clearVaciaLaSeccion() {
        cache.put(1L, "a");
        cache.put(2L, "b");

        cache.clear();

        assertNull(real.get(1L));
        assertNull(real.get(2L));
    }

    @Test
    void elManagerEnvuelveCadaSeccionUnaSolaVez() {
        LoggingCacheManager manager = new LoggingCacheManager(new ConcurrentMapCacheManager("clientes"));

        assertInstanceOf(LoggingCache.class, manager.getCache("clientes"));
        assertSame(manager.getCache("clientes"), manager.getCache("clientes"));
        assertTrue(manager.getCacheNames().contains("clientes"));
    }
}
