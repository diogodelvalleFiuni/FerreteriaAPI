package com.ferreteria.compras.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.event.Level;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/** CacheManager que devuelve cada seccion envuelta en {@link LoggingCache} (trazabilidad de get/put/evict). */
public class LoggingCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Level level;
    private final Map<String, Cache> wrapped = new ConcurrentHashMap<>();

    public LoggingCacheManager(CacheManager delegate, Level level) {
        this.delegate = delegate;
        this.level = level;
    }

    @Override
    public Cache getCache(String name) {
        Cache existing = wrapped.get(name);
        if (existing != null) {
            return existing;
        }
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        return wrapped.computeIfAbsent(name, n -> new LoggingCache(cache, level));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
