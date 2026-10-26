package com.ferreteria.ventas.service;

import com.ferreteria.entity.general.Estado;

/**
 * Servicio de apoyo (sin controller): resuelve el estado "inactivo" que usan los borrados logicos
 * de presupuestos y facturas. Es el unico lugar que consulta la tabla estados.
 */
public interface EstadoService {

    /** Id del estado inactivo (configurado por id o buscado por nombre). */
    Long getInactivoId();

    /** Referencia (solo con id, sin consultar la BD) al estado inactivo, lista para asignar a una entidad. */
    Estado getInactivo();
}
