package com.ferreteria.compras.repository;

import com.ferreteria.entity.compras.Proveedor;

/**
 * Repositorio de Proveedor. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface ProveedorRepository extends BaseRepository<Proveedor, Long> {

    /** Hay otro proveedor (distinto de {@code idPersona}) con el mismo RUC. */
    boolean existsByRucAndIdPersonaNot(String ruc, Long idPersona);
}
