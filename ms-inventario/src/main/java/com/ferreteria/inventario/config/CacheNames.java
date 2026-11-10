package com.ferreteria.inventario.config;

/**
 * Secciones (nombres de cache) fijas, una por servicio. Cada seccion guarda DTOs individuales
 * con el id como clave; separarlas evita que, por ejemplo, el id 1 de Producto pise al id 1 de Marca.
 */
public final class CacheNames {

    public static final String CATEGORIAS = "categorias";
    public static final String DEPOSITOS = "depositos";
    public static final String DETALLES_TRANSFERENCIA = "detalles-transferencia";
    public static final String INVENTARIOS = "inventarios";
    public static final String MARCAS = "marcas";
    public static final String PRODUCTOS = "productos";
    public static final String TRANSFERENCIAS = "transferencias";

    private CacheNames() {
    }
}
