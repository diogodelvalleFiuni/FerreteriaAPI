package com.ferreteria.inventario.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;

import com.ferreteria.entity.Inventario.Inventario;

/**
 * Repositorio de Inventario. Hereda CRUD, paginacion y Specification de {@link BaseRepository}.
 */
public interface InventarioRepository extends BaseRepository<Inventario, Long> {

    /** Lectura simple del stock de un producto en un deposito (sin bloqueo). */
    Optional<Inventario> findFirstByDeposito_IdDepositoAndProducto_IdProducto(Long idDeposito, Long idProducto);

    /**
     * Igual que la anterior pero bloquea la fila (SELECT ... FOR UPDATE) hasta el fin de la transaccion, para que dos
     * operaciones simultaneas sobre el mismo stock no se pisen. Exige una transaccion activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventario> findByDeposito_IdDepositoAndProducto_IdProducto(Long idDeposito, Long idProducto);
}
