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

import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.compras.config.CacheNames;
import com.ferreteria.compras.exception.BusinessRuleException;
import com.ferreteria.compras.exception.DuplicateResourceException;
import com.ferreteria.compras.filter.FacturaCompraFilter;
import com.ferreteria.compras.mapper.FacturaCompraMapper;
import com.ferreteria.compras.repository.FacturaCompraRepository;
import com.ferreteria.compras.service.AbstractBaseService;
import com.ferreteria.compras.service.EstadoService;
import com.ferreteria.compras.service.FacturaCompraService;
import com.ferreteria.compras.service.support.EstadoVisibilidad;
import com.ferreteria.entity.compras.FacturaCompra;

/**
 * Facturas de compra.
 * <ul>
 *     <li>Alta: estado "activo" por defecto, importe total inicial 0 (se calcula con los detalles).</li>
 *     <li>Reglas: vencimiento no anterior a la emision; no repetir proveedor + nro de factura + timbrado.</li>
 *     <li>DELETE = borrado logico: se asigna el estado "inactivo" y la factura deja de listarse (404 por id).</li>
 * </ul>
 * Cache (seccion "facturas-compra", clave = id): se invalida al modificar o borrar la factura y cada vez que cambia su
 * importe total (alta, cambio o baja de un detalle, ver {@link #actualizarImporteTotal}). Como los detalles de una
 * factura inactiva dejan de ser visibles, al modificar o borrar una factura tambien se vacia la seccion de detalles.
 */
@Service
public class FacturaCompraServiceImpl
        extends AbstractBaseService<FacturaCompra, FacturaCompraDTO, FacturaCompraFilter, Long, FacturaCompraRepository, FacturaCompraMapper>
        implements FacturaCompraService {

    private final EstadoService estadoService;

    public FacturaCompraServiceImpl(FacturaCompraRepository repository, FacturaCompraMapper mapper,
                                    EstadoService estadoService, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
        this.estadoService = estadoService;
    }

    @Override
    protected String cacheName() {
        return CacheNames.FACTURAS_COMPRA;
    }

    @Override
    protected Object idOf(FacturaCompraDTO dto) {
        return dto.getIdFacturaCompra();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FACTURAS_COMPRA, key = "#id")
    public FacturaCompraDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.FACTURAS_COMPRA, key = "#result.idFacturaCompra")
    public FacturaCompraDTO create(FacturaCompraDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.FACTURAS_COMPRA, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, allEntries = true)
    })
    public FacturaCompraDTO update(Long id, FacturaCompraDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.FACTURAS_COMPRA, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.DETALLES_FACTURA_COMPRA, allEntries = true)
    })
    public void delete(Long id) {
        super.delete(id);
    }

    /** El importe total cambio: la factura guardada en Redis quedo vieja, se invalida. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FACTURAS_COMPRA, key = "#idFacturaCompra")
    public void actualizarImporteTotal(Long idFacturaCompra, BigDecimal importeTotal) {
        FacturaCompra factura = getEntityOrThrow(idFacturaCompra);
        log.info("Factura {}: importe total = {}", idFacturaCompra, importeTotal);
        factura.setImporteTotal(importeTotal);
        repository.save(factura);
    }

    @Override
    protected void beforeSave(FacturaCompra factura, Long id) {
        if (factura.getFechaEmision() != null && factura.getFechaVencimiento() != null
                && factura.getFechaVencimiento().isBefore(factura.getFechaEmision())) {
            throw new BusinessRuleException("La fecha de vencimiento no puede ser anterior a la fecha de emision");
        }
        validarNoDuplicada(factura, id);
        if (id == null) {
            if (factura.getEstado() == null) {
                factura.setEstado(estadoService.obtenerActivo());
            }
            factura.setImporteTotal(BigDecimal.ZERO);
        }
    }

    /** Borrado logico: la factura queda con el estado inactivo. */
    @Override
    protected void removeEntity(FacturaCompra factura) {
        factura.setEstado(estadoService.obtenerInactivo());
        repository.save(factura);
        log.info("Factura {} marcada como inactiva", factura.getIdFacturaCompra());
    }

    @Override
    protected Specification<FacturaCompra> visibilitySpecification() {
        String inactivo = estadoService.nombreInactivo();
        return (root, query, cb) -> EstadoVisibilidad.estadoActivo(root, cb, inactivo);
    }

    @Override
    protected boolean isVisible(FacturaCompra factura) {
        return !EstadoVisibilidad.esInactivo(factura.getEstado(), estadoService.nombreInactivo());
    }

    private void validarNoDuplicada(FacturaCompra factura, Long id) {
        if (factura.getProveedor() == null || factura.getProveedor().getIdPersona() == null
                || factura.getNroFactura() == null || factura.getNroFactura().isBlank()) {
            return;
        }
        Long idProveedor = factura.getProveedor().getIdPersona();
        long excluir = id == null ? -1L : id;
        boolean duplicada = factura.getTimbrado() == null
                ? repository.existsByProveedor_IdPersonaAndNroFacturaAndIdFacturaCompraNot(
                        idProveedor, factura.getNroFactura(), excluir)
                : repository.existsByProveedor_IdPersonaAndNroFacturaAndTimbradoAndIdFacturaCompraNot(
                        idProveedor, factura.getNroFactura(), factura.getTimbrado(), excluir);
        if (duplicada) {
            log.warn("Factura duplicada: proveedor {} nro {}", idProveedor, factura.getNroFactura());
            throw new DuplicateResourceException("Ya existe una factura " + factura.getNroFactura()
                    + " de este proveedor con el mismo timbrado");
        }
    }
}
