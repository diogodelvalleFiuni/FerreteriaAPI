package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.entity.Inventario.Detalle_transferencia;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.DetalleTransferenciaFilter;
import com.ferreteria.inventario.mapper.DetalleTransferenciaMapper;
import com.ferreteria.inventario.repository.DetalleTransferenciaRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.DetalleTransferenciaService;

@Service
public class DetalleTransferenciaServiceImpl
        extends AbstractBaseService<Detalle_transferencia, DetalleTransferenciaDTO, DetalleTransferenciaFilter, Long, DetalleTransferenciaRepository, DetalleTransferenciaMapper>
        implements DetalleTransferenciaService {

    public DetalleTransferenciaServiceImpl(DetalleTransferenciaRepository repository, DetalleTransferenciaMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.DETALLES_TRANSFERENCIA;
    }

    @Override
    protected Object idOf(DetalleTransferenciaDTO dto) {
        return dto.getIdDetTrans();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DETALLES_TRANSFERENCIA, key = "#id")
    public DetalleTransferenciaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.DETALLES_TRANSFERENCIA, key = "#result.idDetTrans")
    public DetalleTransferenciaDTO create(DetalleTransferenciaDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_TRANSFERENCIA, key = "#id")
    public DetalleTransferenciaDTO update(Long id, DetalleTransferenciaDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_TRANSFERENCIA, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
