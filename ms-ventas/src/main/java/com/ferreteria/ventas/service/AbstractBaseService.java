package com.ferreteria.ventas.service;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.ventas.exception.ResourceNotFoundException;
import com.ferreteria.ventas.mapper.GenericMapper;
import com.ferreteria.ventas.repository.BaseRepository;

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
 * Borrado: por defecto {@link #removeEntity(Object)} borra la fila (las entidades sin columna de
 * estado, como Cliente o los detalles, no admiten otra cosa). Las entidades que si tienen estado
 * usan {@link AbstractLogicalDeleteService}, que lo reemplaza por un borrado logico.
 * <p>
 * Cache (Redis): el CRUD individual se cachea con anotaciones en cada servicio concreto
 * ({@code @Cacheable} en findById, {@code @CachePut} en create, {@code @CacheEvict} en update/delete), con una
 * seccion fija por servicio y el id como clave. {@link #findAll} y {@link #search} NO usan anotaciones: la pagina en
 * si no se cachea (consigna), pero cada DTO que contiene se inserta a mano en la cache ({@link #cacheResults}).
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
        Page<D> result = repository.findAll(pageable).map(mapper::toDto);
        cacheResults(result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<D> search(F filter, Pageable pageable) {
        log.info("Buscando con filtro: {}", filter);
        Page<D> result = repository.findAll(buildSpecification(filter), pageable).map(mapper::toDto);
        cacheResults(result);
        return result;
    }

    /**
     * Cache manual: recorre la pagina e inserta (put) cada DTO por separado con su id como clave,
     * para que un {@code findById} posterior ya lo encuentre en Redis. Un fallo de Redis no rompe la consulta.
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
        E saved = repository.save(mapper.toEntity(dto));
        log.info("Registro creado");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public D update(ID id, D dto) {
        log.info("Actualizando registro {}", id);
        E entity = getEntityOrThrow(id);
        mapper.updateEntityFromDto(dto, entity);
        E saved = repository.save(entity);
        log.info("Registro {} actualizado", id);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(ID id) {
        log.info("Eliminando registro {}", id);
        removeEntity(getEntityOrThrow(id));
        log.info("Registro {} eliminado", id);
    }

    /** Punto de extension: por defecto borra la fila; sobreescribir para borrado logico. */
    protected void removeEntity(E entity) {
        repository.delete(entity);
    }

    /** Punto de extension: arma el Specification de la busqueda (un filtro nulo trae todo). */
    protected Specification<E> buildSpecification(F filter) {
        return filter == null
                ? (root, query, cb) -> cb.conjunction()
                : filter.toSpecification();
    }

    protected E getEntityOrThrow(ID id) {
        return repository.findById(id).orElseThrow(() -> {
            log.warn("Registro {} no encontrado", id);
            return new ResourceNotFoundException("Registro con id " + id + " no encontrado");
        });
    }
}
