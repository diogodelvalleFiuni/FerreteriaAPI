package com.ferreteria.compras.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ferreteria.compras.exception.BusinessRuleException;

/**
 * Calculo de IVA y subtotal de una linea de compra. Todo configurable (app.compras.iva.*, app.compras.monto.escala).
 * <ul>
 *     <li>{@code incluidoEnPrecio=true}: subtotal = cantidad x precio; IVA = subtotal x p / (100 + p)</li>
 *     <li>{@code incluidoEnPrecio=false}: base = cantidad x precio; IVA = base x p / 100; subtotal = base + IVA</li>
 * </ul>
 * En ambos casos el subtotal es el total de la linea (IVA incluido) y la suma de subtotales es el importe de la factura.
 */
@Component
public class IvaCalculator {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final Set<Integer> porcentajesValidos;
    private final int porcentajePorDefecto;
    private final boolean incluidoEnPrecio;
    private final int escala;

    public IvaCalculator(
            @Value("${app.compras.iva.porcentajes-validos}") List<Integer> porcentajesValidos,
            @Value("${app.compras.iva.porcentaje-por-defecto}") int porcentajePorDefecto,
            @Value("${app.compras.iva.incluido-en-precio}") boolean incluidoEnPrecio,
            @Value("${app.compras.monto.escala}") int escala) {
        this.porcentajesValidos = Set.copyOf(porcentajesValidos);
        this.porcentajePorDefecto = porcentajePorDefecto;
        this.incluidoEnPrecio = incluidoEnPrecio;
        this.escala = escala;
    }

    /** Devuelve el porcentaje a usar (el por defecto si viene {@code null}) o falla si no esta permitido. */
    public int resolverPorcentaje(Integer porcentaje) {
        int resuelto = porcentaje == null ? porcentajePorDefecto : porcentaje;
        if (!porcentajesValidos.contains(resuelto)) {
            throw new BusinessRuleException(
                    "Porcentaje de IVA no permitido: " + resuelto + ". Valores validos: " + porcentajesValidos);
        }
        return resuelto;
    }

    public BigDecimal montoIva(int cantidad, BigDecimal precioUnitario, int porcentaje) {
        BigDecimal bruto = bruto(cantidad, precioUnitario);
        BigDecimal pct = BigDecimal.valueOf(porcentaje);
        BigDecimal divisor = incluidoEnPrecio ? CIEN.add(pct) : CIEN;
        return bruto.multiply(pct).divide(divisor, escala, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotal(int cantidad, BigDecimal precioUnitario, int porcentaje) {
        BigDecimal bruto = bruto(cantidad, precioUnitario).setScale(escala, RoundingMode.HALF_UP);
        return incluidoEnPrecio ? bruto : bruto.add(montoIva(cantidad, precioUnitario, porcentaje));
    }

    private BigDecimal bruto(int cantidad, BigDecimal precioUnitario) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
