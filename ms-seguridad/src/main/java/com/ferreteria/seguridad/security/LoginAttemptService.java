package com.ferreteria.seguridad.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ferreteria.seguridad.exception.TooManyAttemptsException;

/**
 * Freno a la fuerza bruta sobre el login: tras {@code maxAttempts} fallos seguidos de la misma combinacion
 * IP + usuario, esa combinacion queda bloqueada {@code lockMinutes}. Se bloquea por IP+usuario (y no solo por usuario)
 * para que un atacante no pueda dejar fuera a un usuario real. Cuenta igual usuarios inexistentes, asi no sirve
 * para averiguar cuales existen. El estado vive en memoria: se pierde al reiniciar y no se comparte entre
 * instancias (para varias instancias habria que moverlo a Redis).
 */
@Service
public class LoginAttemptService {

    private static final int PURGE_THRESHOLD = 10_000;

    private record Entry(int failures, long lockedUntilMillis, long lastFailureMillis) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMillis;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(LoginLimitProperties props) {
        this(props, Clock.systemUTC());
    }

    LoginAttemptService(LoginLimitProperties props, Clock clock) {
        this.maxAttempts = Math.max(1, props.maxAttempts());
        this.lockMillis = Math.max(1, props.lockMinutes()) * 60_000L;
        this.clock = clock;
    }

    /** @throws TooManyAttemptsException si la clave esta bloqueada */
    public void checkAllowed(String key) {
        Entry entry = entries.get(key);
        long now = clock.millis();
        if (entry != null && entry.lockedUntilMillis() > now) {
            long remaining = (entry.lockedUntilMillis() - now + 999) / 1000;
            throw new TooManyAttemptsException(remaining);
        }
    }

    public void recordFailure(String key) {
        long now = clock.millis();
        if (entries.size() > PURGE_THRESHOLD) {
            entries.entrySet().removeIf(e -> isStale(e.getValue(), now));
        }
        entries.compute(key, (k, current) -> {
            int failures = (current == null || isStale(current, now)) ? 1 : current.failures() + 1;
            long lockedUntil = failures >= maxAttempts ? now + lockMillis : 0L;
            return new Entry(failures, lockedUntil, now);
        });
    }

    public void recordSuccess(String key) {
        entries.remove(key);
    }

    /** Entrada sin bloqueo vigente y con el ultimo fallo fuera de la ventana: ya no cuenta. */
    private boolean isStale(Entry entry, long now) {
        return entry.lockedUntilMillis() <= now && now - entry.lastFailureMillis() > lockMillis;
    }
}
