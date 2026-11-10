package com.ferreteria.ventas.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

/**
 * Decorador que registra en el log (logger {@code com.ferreteria.ventas.cache}) cada operacion sobre la cache:
 * GET (con HIT/MISS), PUT, EVICT y CLEAR, indicando seccion y clave. Nunca se loguea el valor (puede traer datos
 * personales del cliente). Las anotaciones (@Cacheable, @CachePut, @CacheEvict) y el cacheo manual de las busquedas
 * pasan por aca, asi que quedan cubiertos todos los caminos.
 */
public class LoggingCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(LoggingCache.class);

    private final Cache delegate;

    public LoggingCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        log.info("CACHE GET {} key={} -> {}", getName(), key, value != null ? "HIT" : "MISS");
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = delegate.get(key, type);
        log.info("CACHE GET {} key={} -> {}", getName(), key, value != null ? "HIT" : "MISS");
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.info("CACHE GET {} key={} (carga si falta)", getName(), key);
        return delegate.get(key, valueLoader);
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        log.info("CACHE RETRIEVE {} key={}", getName(), key);
        return delegate.retrieve(key);
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        log.info("CACHE RETRIEVE {} key={} (carga si falta)", getName(), key);
        return delegate.retrieve(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        log.info("CACHE PUT {} key={}", getName(), key);
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        log.info("CACHE PUT-IF-ABSENT {} key={}", getName(), key);
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        log.info("CACHE EVICT {} key={}", getName(), key);
        delegate.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean removed = delegate.evictIfPresent(key);
        log.info("CACHE EVICT {} key={} -> {}", getName(), key, removed ? "ELIMINADA" : "NO EXISTIA");
        return removed;
    }

    @Override
    public void clear() {
        log.info("CACHE CLEAR {}", getName());
        delegate.clear();
    }

    @Override
    public boolean invalidate() {
        boolean had = delegate.invalidate();
        log.info("CACHE INVALIDATE {} -> {}", getName(), had ? "CON DATOS" : "VACIA");
        return had;
    }
}
