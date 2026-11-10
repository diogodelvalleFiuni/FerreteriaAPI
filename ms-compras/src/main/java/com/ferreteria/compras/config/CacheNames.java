package com.ferreteria.compras.config;

/**
 * Secciones (nombres de cache) fijas, una por servicio de dominio. Cada seccion guarda DTOs individuales con el id
 * como clave; separarlas permite invalidar una sola seccion sin tocar las demas.
 * Claves resultantes en Redis: {@code compras::proveedores::<id>}, {@code compras::facturas-compra::<id>}, etc.
 */
public final class CacheNames {

    public static final String PROVEEDORES = "proveedores";
    public static final String FACTURAS_COMPRA = "facturas-compra";
    public static final String DETALLES_FACTURA_COMPRA = "detalles-factura-compra";

    private CacheNames() {
    }
}
