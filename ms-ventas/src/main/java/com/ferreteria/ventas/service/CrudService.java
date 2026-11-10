package com.ferreteria.ventas.service;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Operaciones CRUD comunes que expone todo servicio de dominio.
 * <p>
 * {@link AbstractBaseService} la implementa; cada modulo declara su propia
 * interfaz vacia que la extiende con los tipos concretos, ej:
 * <pre>{@code
 * public interface DepositoService extends CrudService<DepositoDTO, DepositoFilter, Long> {
 * }
 * }</pre>
 * Las consultas de multiples registros son siempre paginadas (requisito de la consigna).
 *
 * @param <D>  DTO expuesto por el servicio
 * @param <F>  Filter de busqueda
 * @param <ID> tipo del identificador
 */
public interface CrudService<D, F, ID extends Serializable> {

    Page<D> findAll(Pageable pageable);

    Page<D> search(F filter, Pageable pageable);

    D findById(ID id);

    D create(D dto);

    D update(ID id, D dto);

    /** Elimina el registro (ver {@link AbstractBaseService#removeEntity}). */
    void delete(ID id);
}
