package com.ferreteria.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.compras.exception.BusinessRuleException;
import com.ferreteria.compras.mapper.DetalleFacturaCompraMapper;
import com.ferreteria.compras.repository.DetalleFacturaCompraRepository;
import com.ferreteria.compras.service.impl.DetalleFacturaCompraServiceImpl;
import com.ferreteria.compras.service.support.IvaCalculator;
import com.ferreteria.entity.compras.DetalleFacturaCompra;
import com.ferreteria.entity.compras.FacturaCompra;
import com.ferreteria.entity.general.Producto;

@ExtendWith(MockitoExtension.class)
class DetalleFacturaCompraServiceImplTest {

    @Mock
    private DetalleFacturaCompraRepository repository;
    @Mock
    private DetalleFacturaCompraMapper mapper;
    @Mock
    private FacturaCompraService facturaService;
    @Mock
    private EstadoService estadoService;
    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    private DetalleFacturaCompraServiceImpl service;

    @BeforeEach
    void setUp() {
        IvaCalculator iva = new IvaCalculator(List.of(0, 5, 10), 10, true, 2);
        service = new DetalleFacturaCompraServiceImpl(repository, mapper, facturaService, estadoService, iva, cacheManager);
    }

    private static DetalleFacturaCompra detalle(Integer cantidad, String precio) {
        FacturaCompra factura = new FacturaCompra();
        factura.setIdFacturaCompra(1L);
        Producto producto = new Producto();
        producto.setIdProducto(3L);
        DetalleFacturaCompra detalle = new DetalleFacturaCompra();
        detalle.setFacturaCompra(factura);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        return detalle;
    }

    @Test
    void createCalculaIvaSubtotalYActualizaElTotalDeLaFactura() {
        DetalleFacturaCompraDTO dto = new DetalleFacturaCompraDTO();
        DetalleFacturaCompra entity = detalle(2, "5500");
        BigDecimal totalFactura = new BigDecimal("11000.00");
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(facturaService.findById(1L)).thenReturn(new FacturaCompraDTO());
        when(repository.save(entity)).thenReturn(entity);
        when(repository.sumSubtotalByFactura(1L)).thenReturn(totalFactura);
        when(mapper.toDto(entity)).thenReturn(dto);

        service.create(dto);

        assertEquals(10, entity.getPorcentajeIva());
        assertEquals(0, new BigDecimal("1000").compareTo(entity.getMontoIva()));
        assertEquals(0, new BigDecimal("11000").compareTo(entity.getSubtotal()));
        verify(facturaService).actualizarImporteTotal(1L, totalFactura);
    }

    @Test
    void cantidadCeroLanzaBusinessRuleYNoGuarda() {
        DetalleFacturaCompraDTO dto = new DetalleFacturaCompraDTO();
        when(mapper.toEntity(dto)).thenReturn(detalle(0, "100"));

        assertThrows(BusinessRuleException.class, () -> service.create(dto));
        verify(repository, never()).save(any(DetalleFacturaCompra.class));
    }

    @Test
    void deleteBorraElDetalleYRecalculaElTotal() {
        DetalleFacturaCompra entity = detalle(1, "100");
        when(repository.findById(9L)).thenReturn(Optional.of(entity));
        when(repository.sumSubtotalByFactura(1L)).thenReturn(null);

        service.delete(9L);

        verify(repository).delete(entity);
        verify(facturaService).actualizarImporteTotal(1L, BigDecimal.ZERO);
    }

    @Test
    void noSePermiteMoverUnDetalleAOtraFactura() {
        DetalleFacturaCompra entity = detalle(1, "100");
        DetalleFacturaCompraDTO actual = new DetalleFacturaCompraDTO().idFacturaCompra(1L);
        DetalleFacturaCompraDTO nuevo = new DetalleFacturaCompraDTO().idFacturaCompra(2L);
        when(repository.findById(5L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(actual);

        assertThrows(BusinessRuleException.class, () -> service.update(5L, nuevo));
        verify(repository, never()).save(any(DetalleFacturaCompra.class));
    }
}
