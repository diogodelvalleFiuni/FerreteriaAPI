package com.ferreteria.inventario.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.DepositoDTO;
import com.ferreteria.entity.Inventario.Deposito;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.filter.DepositoFilter;
import com.ferreteria.inventario.mapper.DepositoMapper;
import com.ferreteria.inventario.repository.DepositoRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.DepositoService;

@Service
public class DepositoServiceImpl
        extends AbstractBaseService<Deposito, DepositoDTO, DepositoFilter, Long, DepositoRepository, DepositoMapper>
        implements DepositoService {

    public DepositoServiceImpl(DepositoRepository repository, DepositoMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.DEPOSITOS;
    }

    @Override
    protected Object idOf(DepositoDTO dto) {
        return dto.getIdDeposito();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DEPOSITOS, key = "#id")
    public DepositoDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.DEPOSITOS, key = "#result.idDeposito")
    public DepositoDTO create(DepositoDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DEPOSITOS, key = "#id")
    public DepositoDTO update(Long id, DepositoDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DEPOSITOS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
