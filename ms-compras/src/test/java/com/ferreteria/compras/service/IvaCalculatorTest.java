package com.ferreteria.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ferreteria.compras.exception.BusinessRuleException;
import com.ferreteria.compras.service.support.IvaCalculator;

class IvaCalculatorTest {

    private final IvaCalculator ivaIncluido = new IvaCalculator(List.of(0, 5, 10), 10, true, 2);
    private final IvaCalculator ivaAparte = new IvaCalculator(List.of(0, 5, 10), 10, false, 2);

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void ivaIncluidoEnElPrecio() {
        // 2 x 5.500 = 11.000 con IVA 10% incluido -> IVA = 11.000 / 11 = 1.000
        assertMonto("11000", ivaIncluido.subtotal(2, new BigDecimal("5500"), 10));
        assertMonto("1000", ivaIncluido.montoIva(2, new BigDecimal("5500"), 10));
    }

    @Test
    void ivaCincoPorCientoIncluido() {
        // 21.000 con IVA 5% incluido -> IVA = 21.000 / 21 = 1.000
        assertMonto("1000", ivaIncluido.montoIva(1, new BigDecimal("21000"), 5));
    }

    @Test
    void ivaSumadoAlPrecio() {
        // 2 x 10.000 = 20.000 + IVA 10% (2.000) = 22.000
        assertMonto("2000", ivaAparte.montoIva(2, new BigDecimal("10000"), 10));
        assertMonto("22000", ivaAparte.subtotal(2, new BigDecimal("10000"), 10));
    }

    @Test
    void exentoNoTieneIva() {
        assertMonto("0", ivaIncluido.montoIva(3, new BigDecimal("1234.50"), 0));
        assertMonto("3703.50", ivaIncluido.subtotal(3, new BigDecimal("1234.50"), 0));
    }

    @Test
    void porcentajeNuloUsaElPorDefecto() {
        assertEquals(10, ivaIncluido.resolverPorcentaje(null));
        assertEquals(5, ivaIncluido.resolverPorcentaje(5));
    }

    @Test
    void porcentajeNoPermitidoLanzaBusinessRule() {
        assertThrows(BusinessRuleException.class, () -> ivaIncluido.resolverPorcentaje(7));
    }
}
