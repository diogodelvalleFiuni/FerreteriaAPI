package com.ferreteria.ventas.cache;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/** Envuelve cada seccion del {@link CacheManager} real con {@link LoggingCache} para trazar todas las operaciones. */
public class LoggingCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final Map<String, Cache> wrapped = new ConcurrentHashMap<>();

    public LoggingCacheManager(CacheManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        return wrapped.computeIfAbsent(name, n -> new LoggingCache(cache));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
