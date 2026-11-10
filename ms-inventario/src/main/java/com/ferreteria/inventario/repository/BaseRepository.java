package com.ferreteria.inventario.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositorio base (Spring Data JPA): CRUD + paginacion + busqueda por Specification.
 * Las entidades vienen del JAR commons (com.ferreteria.entity.*), por eso no se exige
 * ninguna superclase comun.
 *
 * @param <E>  tipo de entidad
 * @param <ID> tipo del identificador
 */
@NoRepositoryBean
public interface BaseRepository<E, ID extends Serializable>
        extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {
}
