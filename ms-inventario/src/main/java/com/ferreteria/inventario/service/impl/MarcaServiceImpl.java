package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.MarcaDTO;
import com.ferreteria.entity.general.Marca;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.MarcaFilter;
import com.ferreteria.inventario.mapper.MarcaMapper;
import com.ferreteria.inventario.repository.MarcaRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.MarcaService;

@Service
public class MarcaServiceImpl
        extends AbstractBaseService<Marca, MarcaDTO, MarcaFilter, Long, MarcaRepository, MarcaMapper>
        implements MarcaService {

    public MarcaServiceImpl(MarcaRepository repository, MarcaMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.MARCAS;
    }

    @Override
    protected Object idOf(MarcaDTO dto) {
        return dto.getIdMarca();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.MARCAS, key = "#id")
    public MarcaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.MARCAS, key = "#result.idMarca")
    public MarcaDTO create(MarcaDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.MARCAS, key = "#id")
    public MarcaDTO update(Long id, MarcaDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.MARCAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
