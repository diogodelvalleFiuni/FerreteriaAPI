package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.entity.Inventario.Inventario;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.InventarioFilter;
import com.ferreteria.inventario.mapper.InventarioMapper;
import com.ferreteria.inventario.repository.InventarioRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.InventarioService;

@Service
public class InventarioServiceImpl
        extends AbstractBaseService<Inventario, InventarioDTO, InventarioFilter, Long, InventarioRepository, InventarioMapper>
        implements InventarioService {

    public InventarioServiceImpl(InventarioRepository repository, InventarioMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.INVENTARIOS;
    }

    @Override
    protected Object idOf(InventarioDTO dto) {
        return dto.getId();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public InventarioDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.INVENTARIOS, key = "#result.id")
    public InventarioDTO create(InventarioDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public InventarioDTO update(Long id, InventarioDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
