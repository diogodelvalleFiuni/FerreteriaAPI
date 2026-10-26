package com.ferreteria.ventas.repository;

import com.ferreteria.entity.ventas.Cliente;

/**
 * Repositorio de Cliente. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface ClienteRepository extends BaseRepository<Cliente, Long> {

    /** Para validar que el CI no se repita al crear. */
    boolean existsByCi(String ci);

    /** Para validar que el CI no se repita al actualizar (excluye al propio cliente). */
    boolean existsByCiAndIdPersonaNot(String ci, Long idPersona);
}
