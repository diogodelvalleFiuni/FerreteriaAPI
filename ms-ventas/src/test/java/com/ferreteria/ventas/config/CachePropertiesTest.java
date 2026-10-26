package com.ferreteria.ventas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Politica de retencion: TTL propio por seccion, TTL por defecto y 0 = persistente. */
class CachePropertiesTest {

    private final CacheProperties props = new CacheProperties("ventas::", 10,
            Map.of(CacheNames.CLIENTES, 30L, CacheNames.FACTURAS_VENTAS, 0L));

    @Test
    void usaElTtlPropioDeLaSeccion() {
        assertEquals(Duration.ofMinutes(30), props.ttlFor(CacheNames.CLIENTES));
    }

    @Test
    void ceroSignificaPersistente() {
        assertEquals(Duration.ZERO, props.ttlFor(CacheNames.FACTURAS_VENTAS));
    }

    @Test
    void unaSeccionSinValorUsaElTtlPorDefecto() {
        assertEquals(Duration.ofMinutes(10), props.ttlFor(CacheNames.PRESUPUESTOS));
    }
}
