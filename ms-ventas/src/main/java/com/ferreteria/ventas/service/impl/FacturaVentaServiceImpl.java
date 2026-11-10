package com.ferreteria.ventas.service.impl;

import java.time.LocalDate;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.entity.general.Estado;
import com.ferreteria.entity.ventas.FacturaVenta;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.exception.BusinessException;
import com.ferreteria.ventas.filter.FacturaVentaFilter;
import com.ferreteria.ventas.mapper.FacturaVentaMapper;
import com.ferreteria.ventas.repository.FacturaVentaRepository;
import com.ferreteria.ventas.service.AbstractLogicalDeleteService;
import com.ferreteria.ventas.service.EstadoService;
import com.ferreteria.ventas.service.FacturaVentaService;

/**
 * DELETE = pasar la factura al estado inactivo (la entrada de la cache se invalida igual). Reglas de negocio al
 * crear/actualizar:
 * <ul>
 *     <li>una factura a credito requiere fecha de vencimiento</li>
 *     <li>el vencimiento no puede ser anterior a la emision</li>
 *     <li>importe total e IVA no pueden ser negativos</li>
 * </ul>
 */
@Service
public class FacturaVentaServiceImpl
        extends AbstractLogicalDeleteService<FacturaVenta, FacturaVentaDTO, FacturaVentaFilter, Long, FacturaVentaRepository, FacturaVentaMapper>
        implements FacturaVentaService {

    public FacturaVentaServiceImpl(FacturaVentaRepository repository, FacturaVentaMapper mapper,
                                   EstadoService estadoService, CacheManager cacheManager) {
        super(repository, mapper, estadoService, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.FACTURAS_VENTAS;
    }

    @Override
    protected Object idOf(FacturaVentaDTO dto) {
        return dto.getIdFactura();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FACTURAS_VENTAS, key = "#id")
    public FacturaVentaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: valida, guarda y deja el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.FACTURAS_VENTAS, key = "#result.idFactura")
    public FacturaVentaDTO create(FacturaVentaDTO dto) {
        validar(dto);
        return super.create(dto);
    }

    /** Actualizacion: valida e invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FACTURAS_VENTAS, key = "#id")
    public FacturaVentaDTO update(Long id, FacturaVentaDTO dto) {
        validar(dto);
        return super.update(id, dto);
    }

    /** Borrado: invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FACTURAS_VENTAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }

    @Override
    protected Estado getEstado(FacturaVenta entity) {
        return entity.getEstado();
    }

    @Override
    protected void setEstado(FacturaVenta entity, Estado estado) {
        entity.setEstado(estado);
    }

    private void validar(FacturaVentaDTO dto) {
        log.debug("Validando reglas de negocio de la factura");
        LocalDate emision = dto.getFechaEmision();
        LocalDate vencimiento = dto.getFechaVencimiento();
        if (Boolean.TRUE.equals(dto.getEsCredito()) && vencimiento == null) {
            throw new BusinessException("Una factura a credito requiere fecha de vencimiento");
        }
        if (emision != null && vencimiento != null && vencimiento.isBefore(emision)) {
            throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha de emision");
        }
        if ((dto.getImporteTotal() != null && dto.getImporteTotal() < 0)
                || (dto.getIva() != null && dto.getIva() < 0)) {
            throw new BusinessException("El importe total y el IVA no pueden ser negativos");
        }
    }
}
