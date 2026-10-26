package com.ferreteria.ventas.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.PresupuestoDetalleDTO;
import com.ferreteria.entity.ventas.PresupuestoDetalle;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.filter.PresupuestoDetalleFilter;
import com.ferreteria.ventas.mapper.PresupuestoDetalleMapper;
import com.ferreteria.ventas.repository.PresupuestoDetalleRepository;
import com.ferreteria.ventas.service.AbstractBaseService;
import com.ferreteria.ventas.service.PresupuestoDetalleService;

/**
 * La tabla presupuestos_detalle no tiene columna de estado, por eso el DELETE elimina la fila
 * (comportamiento por defecto de {@link AbstractBaseService}).
 */
@Service
public class PresupuestoDetalleServiceImpl
        extends AbstractBaseService<PresupuestoDetalle, PresupuestoDetalleDTO, PresupuestoDetalleFilter, Long, PresupuestoDetalleRepository, PresupuestoDetalleMapper>
        implements PresupuestoDetalleService {

    public PresupuestoDetalleServiceImpl(PresupuestoDetalleRepository repository, PresupuestoDetalleMapper mapper,
                                         CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.PRESUPUESTOS_DETALLE;
    }

    @Override
    protected Object idOf(PresupuestoDetalleDTO dto) {
        return dto.getIdPresupuestoDetalle();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRESUPUESTOS_DETALLE, key = "#id")
    public PresupuestoDetalleDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.PRESUPUESTOS_DETALLE, key = "#result.idPresupuestoDetalle")
    public PresupuestoDetalleDTO create(PresupuestoDetalleDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRESUPUESTOS_DETALLE, key = "#id")
    public PresupuestoDetalleDTO update(Long id, PresupuestoDetalleDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRESUPUESTOS_DETALLE, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
