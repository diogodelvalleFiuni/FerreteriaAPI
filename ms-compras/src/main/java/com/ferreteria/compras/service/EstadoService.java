package com.ferreteria.compras.service;

import com.ferreteria.entity.general.Estado;

/**
 * Servicio de apoyo (sin controller) para el borrado logico: entrega los estados "activo" e "inactivo"
 * configurados en {@code app.compras.estado.*}.
 */
public interface EstadoService {

    /** Estado "activo"; se crea en la tabla estados si todavia no existe. */
    Estado obtenerActivo();

    /** Estado "inactivo"; se crea en la tabla estados si todavia no existe. */
    Estado obtenerInactivo();

    /** Nombre configurado del estado inactivo (para filtrar sin consultar la base). */
    String nombreInactivo();
}
