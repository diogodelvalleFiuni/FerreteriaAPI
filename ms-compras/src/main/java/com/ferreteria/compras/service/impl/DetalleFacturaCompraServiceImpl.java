package com.ferreteria.compras.service.impl;

import java.math.BigDecimal;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.compras.config.CacheNames;
import com.ferreteria.compras.exception.BusinessRuleException;
import com.ferreteria.compras.filter.DetalleFacturaCompraFilter;
import com.ferreteria.compras.mapper.DetalleFacturaCompraMapper;
import com.ferreteria.compras.repository.DetalleFacturaCompraRepository;
import com.ferreteria.compras.service.AbstractBaseService;
import com.ferreteria.compras.service.DetalleFacturaCompraService;
import com.ferreteria.compras.service.EstadoService;
import com.ferreteria.compras.service.FacturaCompraService;
import com.ferreteria.compras.service.support.EstadoVisibilidad;
import com.ferreteria.compras.service.support.IvaCalculator;
import com.ferreteria.entity.compras.DetalleFacturaCompra;

import jakarta.persistence.criteria.Join;

/**
 * Detalles (lineas) de una factura de compra: relacion muchos-a-muchos FacturaCompra - Producto.
 * <ul>
 *     <li>La factura debe existir y estar activa (si no, 404).</li>
 *     <li>Monto de IVA y subtotal los calcula el servidor ({@link IvaCalculator}); lo que envie el cliente se ignora.</li>
 *     <li>Tras cada alta, modificacion o baja se recalcula el importe total de la factura.</li>
 *     <li>Un detalle no puede cambiarse de factura (se elimina y se crea otro).</li>
 *     <li>Cache (seccion "detalles-factura-compra", clave = id): el recalculo del importe invalida la factura
 *         ({@link FacturaCompraService#actualizarImporteTotal}), asi no se sirve un total viejo.</li>
 *     <li>Los detalles de facturas inactivas no se listan. Como la tabla no tiene estado propio, el DELETE
 *         de un detalle borra la fila.</li>
 * </ul>
 */
@Service
public class DetalleFacturaCompraServiceImpl
        extends AbstractBaseService<DetalleFacturaCompra, DetalleFacturaCompraDTO, DetalleFacturaCompraFilter, Long,
                DetalleFacturaCompraRepository, DetalleFacturaCompraMapper>
        implements DetalleFacturaCompraService {

    private final FacturaCompraService facturaService;
    private final EstadoService estadoService;
    private final IvaCalculator ivaCalculator;

    public DetalleFacturaCompraServiceImpl(DetalleFacturaCompraRepository repository,
                                           DetalleFacturaCompraMapper mapper,
                                           FacturaCompraService facturaService,
                                           EstadoService estadoService,
                                           IvaCalculator ivaCalculator,
                                           CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
        this.facturaService = facturaService;
        this.estadoService = estadoService;
        this.ivaCalculator = ivaCalculator;
    }


    @Override
    protected String cacheName() {
        return CacheNames.DETALLES_FACTURA_COMPRA;
    }

    @Override
    protected Object idOf(DetalleFacturaCompraDTO dto) {
        return dto.getId();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, key = "#id")
    public DetalleFacturaCompraDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, key = "#result.id")
    public DetalleFacturaCompraDTO create(DetalleFacturaCompraDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada (y, por el recalculo del importe, la factura). */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, key = "#id")
    public DetalleFacturaCompraDTO update(Long id, DetalleFacturaCompraDTO dto) {
        DetalleFacturaCompraDTO actual = findById(id);
        if (dto.getIdFacturaCompra() != null && !dto.getIdFacturaCompra().equals(actual.getIdFacturaCompra())) {
            throw new BusinessRuleException(
                    "Un detalle no se puede mover a otra factura: elimine el detalle y cree uno nuevo");
        }
        return super.update(id, dto);
    }

    /** Borrado: invalida el detalle (y, por el recalculo del importe, la factura). */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }

    @Override
    protected void beforeSave(DetalleFacturaCompra detalle, Long id) {
        Long idFactura = idFactura(detalle);
        if (idFactura == null) {
            throw new BusinessRuleException("La factura de compra es obligatoria");
        }
        if (detalle.getProducto() == null || detalle.getProducto().getIdProducto() == null) {
            throw new BusinessRuleException("El producto es obligatorio");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor a cero");
        }
        if (detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario().signum() < 0) {
            throw new BusinessRuleException("El precio unitario es obligatorio y no puede ser negativo");
        }
        facturaService.findById(idFactura); // 404 si la factura no existe o esta inactiva

        int porcentaje = ivaCalculator.resolverPorcentaje(detalle.getPorcentajeIva());
        detalle.setPorcentajeIva(porcentaje);
        detalle.setMontoIva(ivaCalculator.montoIva(detalle.getCantidad(), detalle.getPrecioUnitario(), porcentaje));
        detalle.setSubtotal(ivaCalculator.subtotal(detalle.getCantidad(), detalle.getPrecioUnitario(), porcentaje));
        log.debug("Detalle de factura {}: IVA {}% = {}, subtotal = {}",
                idFactura, porcentaje, detalle.getMontoIva(), detalle.getSubtotal());
    }

    @Override
    protected void afterSave(DetalleFacturaCompra saved) {
        recalcularImporteFactura(idFactura(saved));
    }

    @Override
    protected void afterRemove(DetalleFacturaCompra removed) {
        recalcularImporteFactura(idFactura(removed));
    }

    @Override
    protected Specification<DetalleFacturaCompra> visibilitySpecification() {
        String inactivo = estadoService.nombreInactivo();
        return (root, query, cb) -> {
            Join<Object, Object> factura = root.join("facturaCompra");
            return EstadoVisibilidad.estadoActivo(factura, cb, inactivo);
        };
    }

    @Override
    protected boolean isVisible(DetalleFacturaCompra detalle) {
        return detalle.getFacturaCompra() == null
                || !EstadoVisibilidad.esInactivo(detalle.getFacturaCompra().getEstado(), estadoService.nombreInactivo());
    }

    private void recalcularImporteFactura(Long idFactura) {
        BigDecimal total = repository.sumSubtotalByFactura(idFactura);
        facturaService.actualizarImporteTotal(idFactura, total == null ? BigDecimal.ZERO : total);
    }

    private static Long idFactura(DetalleFacturaCompra detalle) {
        return detalle.getFacturaCompra() == null ? null : detalle.getFacturaCompra().getIdFacturaCompra();
    }
}
