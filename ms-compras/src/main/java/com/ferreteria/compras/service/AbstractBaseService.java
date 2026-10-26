package com.ferreteria.compras.service;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.compras.exception.ResourceNotFoundException;
import com.ferreteria.compras.mapper.GenericMapper;
import com.ferreteria.compras.repository.BaseRepository;

/**
 * Servicio base con las operaciones CRUD comunes.
 * <ul>
 *     <li>{@code E} - Entidad JPA (del JAR commons)</li>
 *     <li>{@code D} - DTO (del JAR commons)</li>
 *     <li>{@code F} - Filter de busqueda ({@link SpecificationFilter})</li>
 *     <li>{@code ID} - Tipo del identificador</li>
 *     <li>{@code R} - Repository, extiende {@link BaseRepository}</li>
 *     <li>{@code M} - Mapper, extiende {@link GenericMapper}</li>
 * </ul>
 * <h3>Puntos de extension (template method)</h3>
 * Los servicios concretos sobreescriben solo lo que necesitan:
 * <ul>
 *     <li>{@link #beforeSave(Object, Serializable)} - validaciones y calculos antes de guardar (alta y modificacion)</li>
 *     <li>{@link #afterSave(Object)} - efectos posteriores a guardar (ej. recalcular totales)</li>
 *     <li>{@link #removeEntity(Object)} - como se borra: por defecto borra la fila; para borrado logico se
 *         sobreescribe y se marca el registro como inactivo</li>
 *     <li>{@link #afterRemove(Object)} - efectos posteriores al borrado</li>
 *     <li>{@link #visibilitySpecification()} / {@link #isVisible(Object)} - ocultan los registros inactivos
 *         de listados, busquedas y consultas por id (404)</li>
 * </ul>
 * Cache (Redis): el CRUD individual se cachea con anotaciones en cada servicio concreto
 * ({@code @Cacheable} en findById, {@code @CachePut} en create, {@code @CacheEvict} en update/delete), con una
 * seccion fija por servicio y el id como clave. {@link #search} y {@link #findAll} NO cachean la pagina: {@link #search}
 * inserta cada DTO de la pagina por separado (ver {@link #cacheResults}).
 * <p>
 * Logs: INFO para los pasos clave de negocio, DEBUG para el detalle.
 */
public abstract class AbstractBaseService<
        E,
        D,
        F extends SpecificationFilter<E>,
        ID extends Serializable,
        R extends BaseRepository<E, ID>,
        M extends GenericMapper<E, D>> implements CrudService<D, F, ID> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final R repository;
    protected final M mapper;
    protected final CacheManager cacheManager;

    protected AbstractBaseService(R repository, M mapper, CacheManager cacheManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
    }

    /** Seccion (nombre de cache) fija del servicio, ver {@code CacheNames}. */
    protected abstract String cacheName();

    /** Id del DTO, usado como clave en la cache (cada DTO nombra distinto su id). */
    protected abstract Object idOf(D dto);

    @Override
    @Transactional(readOnly = true)
    public Page<D> findAll(Pageable pageable) {
        log.info("Listando registros: {}", pageable);
        return repository.findAll(visibilitySpecification(), pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<D> search(F filter, Pageable pageable) {
        log.info("Buscando con filtro: {}", filter);
        Specification<E> spec = visibilitySpecification();
        if (filter != null) {
            spec = spec.and(filter.toSpecification());
        }
        Page<D> result = repository.findAll(spec, pageable).map(mapper::toDto);
        cacheResults(result);
        return result;
    }

    /**
     * Cache manual: recorre la pagina e inserta (put) cada DTO por separado con su id como clave,
     * para que un {@code findById} posterior ya lo encuentre en Redis. La pagina en si NO se cachea.
     * Un fallo de Redis no rompe la busqueda.
     */
    protected void cacheResults(Page<D> page) {
        try {
            Cache cache = cacheManager.getCache(cacheName());
            if (cache == null) {
                log.warn("Cache '{}' no disponible, se omite el cacheo de la pagina", cacheName());
                return;
            }
            for (D dto : page) {
                Object id = idOf(dto);
                if (id != null) {
                    cache.put(id, dto);
                }
            }
            log.debug("{} DTO(s) puestos en la cache '{}'", page.getNumberOfElements(), cacheName());
        } catch (RuntimeException ex) {
            log.warn("No se pudo cachear la pagina en '{}': {}", cacheName(), ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public D findById(ID id) {
        log.debug("Buscando por id {}", id);
        return mapper.toDto(getEntityOrThrow(id));
    }

    @Override
    @Transactional
    public D create(D dto) {
        log.info("Creando registro");
        E entity = mapper.toEntity(dto);
        beforeSave(entity, null);
        E saved = repository.save(entity);
        afterSave(saved);
        log.info("Registro creado");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public D update(ID id, D dto) {
        log.info("Actualizando registro {}", id);
        E entity = getEntityOrThrow(id);
        mapper.updateEntityFromDto(dto, entity);
        beforeSave(entity, id);
        E saved = repository.save(entity);
        afterSave(saved);
        log.info("Registro {} actualizado", id);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(ID id) {
        log.info("Eliminando registro {}", id);
        E entity = getEntityOrThrow(id);
        removeEntity(entity);
        afterRemove(entity);
        log.info("Registro {} eliminado", id);
    }

    /**
     * Validaciones y calculos previos a guardar.
     *
     * @param entity entidad ya mapeada desde el DTO
     * @param id     {@code null} en un alta; el id del registro en una modificacion
     */
    protected void beforeSave(E entity, ID id) {
        // por defecto no hace nada
    }

    /** Efectos posteriores a guardar (misma transaccion). */
    protected void afterSave(E saved) {
        // por defecto no hace nada
    }

    /** Punto de extension: por defecto borra la fila; sobreescribir para borrado logico. */
    protected void removeEntity(E entity) {
        repository.delete(entity);
    }

    /** Efectos posteriores al borrado (misma transaccion). */
    protected void afterRemove(E removed) {
        // por defecto no hace nada
    }

    /** Condicion que deben cumplir los registros para listarse. Por defecto, todos. */
    protected Specification<E> visibilitySpecification() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** Si el registro se considera activo (visible). Debe ser coherente con {@link #visibilitySpecification()}. */
    protected boolean isVisible(E entity) {
        return true;
    }

    protected E getEntityOrThrow(ID id) {
        return repository.findById(id)
                .filter(this::isVisible)
                .orElseThrow(() -> {
                    log.warn("Registro {} no encontrado", id);
                    return new ResourceNotFoundException("Registro con id " + id + " no encontrado");
                });
    }
}
