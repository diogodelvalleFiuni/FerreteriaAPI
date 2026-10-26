package com.ferreteria.compras.config;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.cache.Cache;

/**
 * Decorador que registra cada operacion sobre la cache: seccion, operacion, clave, acierto/fallo y duracion.
 * Nunca registra el valor (puede tener datos de personas ni tokens), solo la clave.
 */
public class LoggingCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(LoggingCache.class);

    private final Cache delegate;
    private final Level level;

    public LoggingCache(Cache delegate, Level level) {
        this.delegate = delegate;
        this.level = level;
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
        long t0 = System.nanoTime();
        ValueWrapper value = delegate.get(key);
        trace("GET", key, value != null ? "HIT" : "MISS", t0);
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        long t0 = System.nanoTime();
        T value = delegate.get(key, type);
        trace("GET", key, value != null ? "HIT" : "MISS", t0);
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        long t0 = System.nanoTime();
        boolean[] loaded = {false};
        T value = delegate.get(key, () -> {
            loaded[0] = true;
            return valueLoader.call();
        });
        trace("GET", key, loaded[0] ? "MISS (cargado)" : "HIT", t0);
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        long t0 = System.nanoTime();
        delegate.put(key, value);
        trace("SET", key, "ok", t0);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        long t0 = System.nanoTime();
        ValueWrapper previous = delegate.putIfAbsent(key, value);
        trace("SET-IF-ABSENT", key, previous == null ? "guardado" : "ya existia", t0);
        return previous;
    }

    @Override
    public void evict(Object key) {
        long t0 = System.nanoTime();
        delegate.evict(key);
        trace("DELETE", key, "ok", t0);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        long t0 = System.nanoTime();
        boolean removed = delegate.evictIfPresent(key);
        trace("DELETE", key, removed ? "eliminada" : "no existia", t0);
        return removed;
    }

    @Override
    public void clear() {
        long t0 = System.nanoTime();
        delegate.clear();
        trace("CLEAR", "*", "ok", t0);
    }

    @Override
    public boolean invalidate() {
        long t0 = System.nanoTime();
        boolean result = delegate.invalidate();
        trace("CLEAR", "*", result ? "ok" : "sin cambios", t0);
        return result;
    }

    private void trace(String op, Object key, String result, long startNanos) {
        long micros = (System.nanoTime() - startNanos) / 1_000;
        log.atLevel(level).log("CACHE {} seccion={} clave={} resultado={} ({} us)", op, getName(), key, result, micros);
    }
}
