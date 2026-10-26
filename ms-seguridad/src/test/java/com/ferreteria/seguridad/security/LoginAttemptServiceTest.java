package com.ferreteria.seguridad.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ferreteria.seguridad.exception.TooManyAttemptsException;

class LoginAttemptServiceTest {

    /** Reloj que se mueve a mano. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(new LoginLimitProperties(3, 10), clock);

    @Test
    void bloqueaTrasLosIntentosPermitidosYDevuelveCuantoEsperar() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> service.checkAllowed("ip|juan"));
            service.recordFailure("ip|juan");
        }
        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class, () -> service.checkAllowed("ip|juan"));
        assertEquals(600, ex.getRetryAfterSeconds());
    }

    @Test
    void elBloqueoNoAfectaAOtroUsuarioNiAOtraIp() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("ip|juan");
        }
        assertDoesNotThrow(() -> service.checkAllowed("ip|ana"));
        assertDoesNotThrow(() -> service.checkAllowed("otra-ip|juan"));
    }

    @Test
    void elBloqueoVenceConElTiempo() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("ip|juan");
        }
        clock.advanceSeconds(601);
        assertDoesNotThrow(() -> service.checkAllowed("ip|juan"));
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        service.recordFailure("ip|juan");
        service.recordFailure("ip|juan");
        service.recordSuccess("ip|juan");
        service.recordFailure("ip|juan");
        service.recordFailure("ip|juan");
        assertDoesNotThrow(() -> service.checkAllowed("ip|juan"));
    }

    @Test
    void losFallosViejosFueraDeLaVentanaNoSeAcumulan() {
        service.recordFailure("ip|juan");
        service.recordFailure("ip|juan");
        clock.advanceSeconds(601);
        service.recordFailure("ip|juan");
        assertDoesNotThrow(() -> service.checkAllowed("ip|juan"));
    }
}
