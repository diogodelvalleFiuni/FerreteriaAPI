package com.ferreteria.compras.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.ProveedorDTO;
import com.ferreteria.compras.config.CacheNames;
import com.ferreteria.compras.exception.DuplicateResourceException;
import com.ferreteria.compras.filter.ProveedorFilter;
import com.ferreteria.compras.mapper.ProveedorMapper;
import com.ferreteria.compras.repository.ProveedorRepository;
import com.ferreteria.compras.service.AbstractBaseService;
import com.ferreteria.compras.service.ProveedorService;
import com.ferreteria.entity.compras.Proveedor;

/**
 * Proveedores. Las entidades de commons no tienen columna de estado en {@code proveedores}/{@code personas},
 * por eso el DELETE borra la fila (si el proveedor tiene facturas, la FK lo rechaza y responde 409).
 */
@Service
public class ProveedorServiceImpl
        extends AbstractBaseService<Proveedor, ProveedorDTO, ProveedorFilter, Long, ProveedorRepository, ProveedorMapper>
        implements ProveedorService {

    public ProveedorServiceImpl(ProveedorRepository repository, ProveedorMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.PROVEEDORES;
    }

    @Override
    protected Object idOf(ProveedorDTO dto) {
        return dto.getIdPersona();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PROVEEDORES, key = "#id")
    public ProveedorDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.PROVEEDORES, key = "#result.idPersona")
    public ProveedorDTO create(ProveedorDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PROVEEDORES, key = "#id")
    })
    public ProveedorDTO update(Long id, ProveedorDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PROVEEDORES, key = "#id")
    })
    public void delete(Long id) {
        super.delete(id);
    }

    @Override
    protected void beforeSave(Proveedor proveedor, Long id) {
        String ruc = proveedor.getRuc();
        if (ruc != null && !ruc.isBlank()
                && repository.existsByRucAndIdPersonaNot(ruc, id == null ? -1L : id)) {
            log.warn("RUC duplicado: {}", ruc);
            throw new DuplicateResourceException("Ya existe un proveedor con el RUC " + ruc);
        }
    }
}
