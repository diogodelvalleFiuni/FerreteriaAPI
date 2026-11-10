package com.ferreteria.ventas.service;

import java.io.Serializable;

import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.entity.general.Estado;
import com.ferreteria.ventas.exception.ResourceNotFoundException;
import com.ferreteria.ventas.mapper.GenericMapper;
import com.ferreteria.ventas.repository.BaseRepository;

/**
 * Variante de {@link AbstractBaseService} para entidades con atributo {@code estado}
 * (Presupuesto, FacturaVenta): el DELETE no borra la fila, la marca con el estado inactivo.
 * <ul>
 *     <li>los registros inactivos no aparecen en listados ni busquedas</li>
 *     <li>consultar, actualizar o volver a borrar un registro inactivo responde 404</li>
 *     <li>un registro sin estado (null) se considera activo</li>
 * </ul>
 * El estado inactivo se obtiene de {@link EstadoService} (configurable).
 * Las subclases solo indican como leer y escribir el estado de su entidad.
 */
public abstract class AbstractLogicalDeleteService<
        E,
        D,
        F extends SpecificationFilter<E>,
        ID extends Serializable,
        R extends BaseRepository<E, ID>,
        M extends GenericMapper<E, D>> extends AbstractBaseService<E, D, F, ID, R, M> {

    /** Nombre del atributo {@code estado} en la entidad (para armar la condicion "activo"). */
    protected static final String ESTADO_ATTRIBUTE = "estado";

    protected final EstadoService estadoService;

    protected AbstractLogicalDeleteService(R repository, M mapper, EstadoService estadoService,
                                           CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
        this.estadoService = estadoService;
    }

    protected abstract Estado getEstado(E entity);

    protected abstract void setEstado(E entity, Estado estado);

    @Override
    @Transactional(readOnly = true)
    public Page<D> findAll(Pageable pageable) {
        log.info("Listando registros activos: {}", pageable);
        Page<D> result = repository.findAll(activeSpecification(), pageable).map(mapper::toDto);
        cacheResults(result);
        return result;
    }

    @Override
    protected Specification<E> buildSpecification(F filter) {
        return activeSpecification().and(super.buildSpecification(filter));
    }

    @Override
    protected E getEntityOrThrow(ID id) {
        E entity = super.getEntityOrThrow(id);
        if (isInactive(entity)) {
            log.warn("Registro {} esta inactivo", id);
            throw new ResourceNotFoundException("Registro con id " + id + " no encontrado");
        }
        return entity;
    }

    @Override
    protected void removeEntity(E entity) {
        setEstado(entity, estadoService.getInactivo());
        repository.save(entity);
    }

    protected boolean isInactive(E entity) {
        Estado estado = getEstado(entity);
        return estado != null && estadoService.getInactivoId().equals(estado.getIdEstado());
    }

    /** Condicion "no esta inactivo": estado nulo o distinto del estado inactivo. */
    protected Specification<E> activeSpecification() {
        Long inactivoId = estadoService.getInactivoId();
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(ESTADO_ATTRIBUTE)),
                cb.notEqual(root.get(ESTADO_ATTRIBUTE).get("idEstado"), inactivoId));
    }
}
