package com.ferreteria.ventas.config;

/**
 * Secciones (dominios) de la cache, una por servicio. Cada seccion guarda DTOs individuales con el id como clave;
 * separarlas evita que, por ejemplo, el id 1 de Cliente pise al id 1 de Presupuesto, y permite invalidar o expirar
 * una seccion sin tocar las demas. El TTL de cada una se configura en {@code app.cache.ttl-minutes.<seccion>}.
 * <p>
 * Clave final en Redis: {@code <app.cache.key-prefix><seccion>::<id>}, ej. {@code ventas::clientes::15}.
 */
public final class CacheNames {

    public static final String CLIENTES = "clientes";
    public static final String PRESUPUESTOS = "presupuestos";
    public static final String PRESUPUESTOS_DETALLE = "presupuestos-detalle";
    public static final String FACTURAS_VENTAS = "facturas-ventas";
    public static final String DETALLES_FACTURAS_VENTAS = "detalles-facturas-ventas";

    private CacheNames() {
    }
}
