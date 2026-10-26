package com.ferreteria.ventas.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.entity.general.Estado;
import com.ferreteria.entity.ventas.Presupuesto;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.filter.PresupuestoFilter;
import com.ferreteria.ventas.mapper.PresupuestoMapper;
import com.ferreteria.ventas.repository.PresupuestoRepository;
import com.ferreteria.ventas.service.AbstractLogicalDeleteService;
import com.ferreteria.ventas.service.EstadoService;
import com.ferreteria.ventas.service.PresupuestoService;

/**
 * DELETE = pasar el presupuesto al estado inactivo (ver {@link AbstractLogicalDeleteService}); la entrada de la
 * cache se invalida igual, para no servir un presupuesto que ya responde 404.
 */
@Service
public class PresupuestoServiceImpl
        extends AbstractLogicalDeleteService<Presupuesto, PresupuestoDTO, PresupuestoFilter, Long, PresupuestoRepository, PresupuestoMapper>
        implements PresupuestoService {

    public PresupuestoServiceImpl(PresupuestoRepository repository, PresupuestoMapper mapper,
                                  EstadoService estadoService, CacheManager cacheManager) {
        super(repository, mapper, estadoService, cacheManager);
    }

    @Override
    protected Estado getEstado(Presupuesto entity) {
        return entity.getEstado();
    }

    @Override
    protected void setEstado(Presupuesto entity, Estado estado) {
        entity.setEstado(estado);
    }

    @Override
    protected String cacheName() {
        return CacheNames.PRESUPUESTOS;
    }

    @Override
    protected Object idOf(PresupuestoDTO dto) {
        return dto.getIdPresupuesto();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRESUPUESTOS, key = "#id")
    public PresupuestoDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.PRESUPUESTOS, key = "#result.idPresupuesto")
    public PresupuestoDTO create(PresupuestoDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRESUPUESTOS, key = "#id")
    public PresupuestoDTO update(Long id, PresupuestoDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRESUPUESTOS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
