package com.ferreteria.inventario.service;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.inventario.exception.ResourceNotFoundException;
import com.ferreteria.inventario.mapper.GenericMapper;
import com.ferreteria.inventario.repository.BaseRepository;

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
 * Borrado: las entidades de commons no tienen columna de estado/activo, por eso
 * {@link #removeEntity(Object)} borra la fila. Cuando una entidad tenga esa columna,
 * el servicio concreto sobreescribe ese metodo para marcarla como inactiva.
 * <p>
 * Cache (Redis): el CRUD individual se cachea con anotaciones en cada servicio concreto
 * ({@code @Cacheable} en findById, {@code @CachePut} en create, {@code @CacheEvict} en update/delete), con una
 * seccion fija por servicio y el id como clave. {@link #search} NO usa anotaciones: inserta cada DTO de la pagina
 * en la cache a mano (ver {@link #cacheResults}); la pagina en si no se cachea.
 * <p>
 * Logs: los pasos clave de negocio se registran en DEBUG (los controllers registran las solicitudes en INFO).
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
        log.debug("Listando registros: {}", pageable);
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<D> search(F filter, Pageable pageable) {
        log.debug("Buscando con filtro: {}", filter);
        Specification<E> spec = filter == null
                ? (root, query, cb) -> cb.conjunction()
                : filter.toSpecification();
        Page<D> result = repository.findAll(spec, pageable).map(mapper::toDto);
        cacheResults(result);
        return result;
    }

    /**
     * Cache manual: recorre la pagina e inserta (put) cada DTO por separado con su id como clave,
     * para que un {@code findById} posterior ya lo encuentre en Redis. Un fallo de Redis no rompe la busqueda.
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
        log.debug("Creando registro");
        E saved = repository.save(mapper.toEntity(dto));
        log.debug("Registro creado");
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public D update(ID id, D dto) {
        log.debug("Actualizando registro {}", id);
        E entity = getEntityOrThrow(id);
        mapper.updateEntityFromDto(dto, entity);
        E saved = repository.save(entity);
        log.debug("Registro {} actualizado", id);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(ID id) {
        log.debug("Eliminando registro {}", id);
        removeEntity(getEntityOrThrow(id));
        log.debug("Registro {} eliminado", id);
    }

    /** Punto de extension: por defecto borra la fila; sobreescribir para borrado logico. */
    protected void removeEntity(E entity) {
        repository.delete(entity);
    }

    protected E getEntityOrThrow(ID id) {
        return repository.findById(id).orElseThrow(() -> {
            log.warn("Registro {} no encontrado", id);
            return new ResourceNotFoundException("Registro con id " + id + " no encontrado");
        });
    }
}
