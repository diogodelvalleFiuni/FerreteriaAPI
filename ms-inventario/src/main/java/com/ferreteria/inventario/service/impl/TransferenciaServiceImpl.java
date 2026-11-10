package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.entity.Inventario.Transferencia;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.TransferenciaFilter;
import com.ferreteria.inventario.mapper.TransferenciaMapper;
import com.ferreteria.inventario.repository.TransferenciaRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.TransferenciaService;

@Service
public class TransferenciaServiceImpl
        extends AbstractBaseService<Transferencia, TransferenciaDTO, TransferenciaFilter, Long, TransferenciaRepository, TransferenciaMapper>
        implements TransferenciaService {

    public TransferenciaServiceImpl(TransferenciaRepository repository, TransferenciaMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.TRANSFERENCIAS;
    }

    @Override
    protected Object idOf(TransferenciaDTO dto) {
        return dto.getIdTransferencia();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public TransferenciaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.TRANSFERENCIAS, key = "#result.idTransferencia")
    public TransferenciaDTO create(TransferenciaDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public TransferenciaDTO update(Long id, TransferenciaDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
