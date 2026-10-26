package com.ferreteria.ventas.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.DetalleFacturaVentaDTO;
import com.ferreteria.entity.ventas.DetalleFacturaVenta;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.filter.DetalleFacturaVentaFilter;
import com.ferreteria.ventas.mapper.DetalleFacturaVentaMapper;
import com.ferreteria.ventas.repository.DetalleFacturaVentaRepository;
import com.ferreteria.ventas.service.AbstractBaseService;
import com.ferreteria.ventas.service.DetalleFacturaVentaService;

/**
 * La tabla detalles_facturas_ventas no tiene columna de estado, por eso el DELETE elimina la fila
 * (comportamiento por defecto de {@link AbstractBaseService}).
 */
@Service
public class DetalleFacturaVentaServiceImpl
        extends AbstractBaseService<DetalleFacturaVenta, DetalleFacturaVentaDTO, DetalleFacturaVentaFilter, Long, DetalleFacturaVentaRepository, DetalleFacturaVentaMapper>
        implements DetalleFacturaVentaService {

    public DetalleFacturaVentaServiceImpl(DetalleFacturaVentaRepository repository, DetalleFacturaVentaMapper mapper,
                                          CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.DETALLES_FACTURAS_VENTAS;
    }

    @Override
    protected Object idOf(DetalleFacturaVentaDTO dto) {
        return dto.getId();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DETALLES_FACTURAS_VENTAS, key = "#id")
    public DetalleFacturaVentaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.DETALLES_FACTURAS_VENTAS, key = "#result.id")
    public DetalleFacturaVentaDTO create(DetalleFacturaVentaDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURAS_VENTAS, key = "#id")
    public DetalleFacturaVentaDTO update(Long id, DetalleFacturaVentaDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURAS_VENTAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }
}
