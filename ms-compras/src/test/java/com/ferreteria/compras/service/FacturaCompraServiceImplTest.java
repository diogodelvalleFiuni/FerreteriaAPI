package com.ferreteria.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.compras.exception.BusinessRuleException;
import com.ferreteria.compras.exception.DuplicateResourceException;
import com.ferreteria.compras.exception.ResourceNotFoundException;
import com.ferreteria.compras.mapper.FacturaCompraMapper;
import com.ferreteria.compras.repository.FacturaCompraRepository;
import com.ferreteria.compras.service.impl.FacturaCompraServiceImpl;
import com.ferreteria.entity.compras.FacturaCompra;
import com.ferreteria.entity.compras.Proveedor;
import com.ferreteria.entity.general.Estado;

@ExtendWith(MockitoExtension.class)
class FacturaCompraServiceImplTest {

    @Mock
    private FacturaCompraRepository repository;
    @Mock
    private FacturaCompraMapper mapper;
    @Mock
    private EstadoService estadoService;
    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    private FacturaCompraServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FacturaCompraServiceImpl(repository, mapper, estadoService, cacheManager);
    }

    private static Estado estado(long id, String nombre) {
        Estado estado = new Estado();
        estado.setIdEstado(id);
        estado.setNombre(nombre);
        return estado;
    }

    private static FacturaCompra factura(Estado estado) {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdPersona(5L);
        FacturaCompra factura = new FacturaCompra();
        factura.setIdFacturaCompra(1L);
        factura.setProveedor(proveedor);
        factura.setNroFactura("001-001-0000123");
        factura.setEstado(estado);
        return factura;
    }

    @Test
    void deleteEsLogico_marcaElEstadoInactivoYNoBorraLaFila() {
        Estado inactivo = estado(2, "Inactivo");
        FacturaCompra factura = factura(estado(1, "Activo"));
        when(repository.findById(1L)).thenReturn(Optional.of(factura));
        when(estadoService.nombreInactivo()).thenReturn("Inactivo");
        when(estadoService.obtenerInactivo()).thenReturn(inactivo);

        service.delete(1L);

        assertSame(inactivo, factura.getEstado());
        verify(repository).save(factura);
        verify(repository, never()).delete(any(FacturaCompra.class));
    }

    @Test
    void facturaInactivaSeTrataComoInexistente() {
        FacturaCompra factura = factura(estado(2, "inactivo"));
        when(repository.findById(1L)).thenReturn(Optional.of(factura));
        when(estadoService.nombreInactivo()).thenReturn("Inactivo");

        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void createAsignaEstadoActivoEImporteCero() {
        Estado activo = estado(1, "Activo");
        FacturaCompraDTO dto = new FacturaCompraDTO();
        FacturaCompra entity = factura(null);
        entity.setIdFacturaCompra(null);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(estadoService.obtenerActivo()).thenReturn(activo);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, service.create(dto));

        assertSame(activo, entity.getEstado());
        assertEquals(BigDecimal.ZERO, entity.getImporteTotal());
    }

    @Test
    void vencimientoAnteriorALaEmisionLanzaBusinessRule() {
        FacturaCompraDTO dto = new FacturaCompraDTO();
        FacturaCompra entity = factura(null);
        entity.setFechaEmision(LocalDate.of(2026, 10, 10));
        entity.setFechaVencimiento(LocalDate.of(2026, 10, 1));
        when(mapper.toEntity(dto)).thenReturn(entity);

        assertThrows(BusinessRuleException.class, () -> service.create(dto));
        verify(repository, never()).save(any(FacturaCompra.class));
    }

    @Test
    void facturaRepetidaDelMismoProveedorLanzaDuplicate() {
        FacturaCompraDTO dto = new FacturaCompraDTO();
        FacturaCompra entity = factura(null);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.existsByProveedor_IdPersonaAndNroFacturaAndIdFacturaCompraNot(5L, "001-001-0000123", -1L))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(dto));
        verify(repository, never()).save(any(FacturaCompra.class));
    }
}
