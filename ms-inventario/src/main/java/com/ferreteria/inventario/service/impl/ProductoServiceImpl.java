package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.ProductoDTO;
import com.ferreteria.entity.general.Producto;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.ProductoFilter;
import com.ferreteria.inventario.mapper.ProductoMapper;
import com.ferreteria.inventario.repository.ProductoRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.ProductoService;

@Service
public class ProductoServiceImpl
        extends AbstractBaseService<Producto, ProductoDTO, ProductoFilter, Long, ProductoRepository, ProductoMapper>
        implements ProductoService {

    public ProductoServiceImpl(ProductoRepository repository, ProductoMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.PRODUCTOS;
    }

    @Override
    protected Object idOf(ProductoDTO dto) {
        return dto.getIdProducto();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCTOS, key = "#id")
    public ProductoDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCTOS, key = "#result.idProducto")
    public ProductoDTO create(ProductoDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCTOS, key = "#id")
    public ProductoDTO update(Long id, ProductoDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCTOS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
