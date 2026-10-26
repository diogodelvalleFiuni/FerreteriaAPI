package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.CategoriaProductoDTO;
import com.ferreteria.entity.general.CategoriaProducto;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.CategoriaProductoFilter;
import com.ferreteria.inventario.mapper.CategoriaProductoMapper;
import com.ferreteria.inventario.repository.CategoriaProductoRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.CategoriaProductoService;

@Service
public class CategoriaProductoServiceImpl
        extends AbstractBaseService<CategoriaProducto, CategoriaProductoDTO, CategoriaProductoFilter, Long, CategoriaProductoRepository, CategoriaProductoMapper>
        implements CategoriaProductoService {

    public CategoriaProductoServiceImpl(CategoriaProductoRepository repository, CategoriaProductoMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.CATEGORIAS;
    }

    @Override
    protected Object idOf(CategoriaProductoDTO dto) {
        return dto.getIdCategoria();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CATEGORIAS, key = "#id")
    public CategoriaProductoDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.CATEGORIAS, key = "#result.idCategoria")
    public CategoriaProductoDTO create(CategoriaProductoDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIAS, key = "#id")
    public CategoriaProductoDTO update(Long id, CategoriaProductoDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORIAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
