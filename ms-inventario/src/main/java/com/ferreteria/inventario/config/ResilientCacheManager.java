package com.ferreteria.inventario.config;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Hace que Redis sea OPCIONAL: cualquier fallo (caido, sin conexion, timeout, dato ilegible) se trata como "no esta en la
 * cache" y el servicio sigue con la base de datos, sin lanzar error.
 * <p>
 * Para no pagar el timeout de Redis en cada peticion, el primer fallo "abre" un corte: durante {@code retryAfter} no se
 * vuelve a consultar Redis (lecturas = miss, escrituras e invalidaciones = no-op). Pasado ese tiempo se prueba de nuevo y,
 * si Redis volvio, la cache retoma sola. Nota: las invalidaciones ({@code @CacheEvict}) que se omitan durante un corte
 * dejan la entrada vieja en Redis hasta que venza su TTL.
 */
public class ResilientCacheManager implements CacheManager {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheManager.class);

    private final CacheManager delegate;
    private final Circuit circuit;

    public ResilientCacheManager(CacheManager delegate, Duration retryAfter) {
        this.delegate = delegate;
        this.circuit = new Circuit(retryAfter);
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        return cache == null ? null : new ResilientCache(cache, circuit);
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    /** Corte compartido por todas las secciones: si Redis esta caido, lo esta para todas. */
    static final class Circuit {

        private final long retryNanos;
        private volatile long openUntil; // 0 = cerrado

        Circuit(Duration retryAfter) {
            this.retryNanos = retryAfter.toNanos();
        }

        boolean isOpen() {
            long until = openUntil;
            return until != 0 && System.nanoTime() - until < 0;
        }

        void failure(String operation, String cacheName, RuntimeException ex) {
            boolean wasOpen = isOpen();
            long until = System.nanoTime() + retryNanos;
            openUntil = until == 0 ? 1 : until;
            if (!wasOpen) {
                log.warn("Redis no disponible ({} en '{}'): se usa solo la base de datos durante {} s. Causa: {}",
                        operation, cacheName, Duration.ofNanos(retryNanos).toSeconds(), ex.getMessage());
            }
        }
    }

    static final class ResilientCache implements Cache {

        private final Cache delegate;
        private final Circuit circuit;

        ResilientCache(Cache delegate, Circuit circuit) {
            this.delegate = delegate;
            this.circuit = circuit;
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
            if (circuit.isOpen()) {
                return null;
            }
            try {
                return delegate.get(key);
            } catch (RuntimeException ex) {
                circuit.failure("get", getName(), ex);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            if (circuit.isOpen()) {
                return null;
            }
            try {
                return delegate.get(key, type);
            } catch (RuntimeException ex) {
                circuit.failure("get", getName(), ex);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            if (circuit.isOpen()) {
                return load(key, valueLoader);
            }
            try {
                return delegate.get(key, valueLoader);
            } catch (ValueRetrievalException ex) {
                throw ex; // fallo del propio loader (la base), no de Redis
            } catch (RuntimeException ex) {
                circuit.failure("get", getName(), ex);
                return load(key, valueLoader);
            }
        }

        @Override
        public void put(Object key, Object value) {
            if (circuit.isOpen()) {
                return;
            }
            try {
                delegate.put(key, value);
            } catch (RuntimeException ex) {
                circuit.failure("put", getName(), ex);
            }
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            if (circuit.isOpen()) {
                return null;
            }
            try {
                return delegate.putIfAbsent(key, value);
            } catch (RuntimeException ex) {
                circuit.failure("putIfAbsent", getName(), ex);
                return null;
            }
        }

        @Override
        public void evict(Object key) {
            evictIfPresent(key);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            if (circuit.isOpen()) {
                return false;
            }
            try {
                return delegate.evictIfPresent(key);
            } catch (RuntimeException ex) {
                circuit.failure("evict", getName(), ex);
                return false;
            }
        }

        @Override
        public void clear() {
            invalidate();
        }

        @Override
        public boolean invalidate() {
            if (circuit.isOpen()) {
                return false;
            }
            try {
                return delegate.invalidate();
            } catch (RuntimeException ex) {
                circuit.failure("clear", getName(), ex);
                return false;
            }
        }

        private <T> T load(Object key, Callable<T> valueLoader) {
            try {
                return valueLoader.call();
            } catch (Exception ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }
        }
    }
}
