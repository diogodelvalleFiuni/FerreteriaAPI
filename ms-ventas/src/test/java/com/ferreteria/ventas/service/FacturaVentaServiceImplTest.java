package com.ferreteria.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.entity.ventas.FacturaVenta;
import com.ferreteria.ventas.exception.BusinessException;
import com.ferreteria.ventas.mapper.FacturaVentaMapper;
import com.ferreteria.ventas.repository.FacturaVentaRepository;
import com.ferreteria.ventas.service.impl.FacturaVentaServiceImpl;

@ExtendWith(MockitoExtension.class)
class FacturaVentaServiceImplTest {

    @Mock
    private FacturaVentaRepository repository;
    @Mock
    private FacturaVentaMapper mapper;
    @Mock
    private EstadoService estadoService;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private FacturaVentaServiceImpl service;

    @Test
    void facturaACreditoSinVencimientoEsRechazada() {
        FacturaVentaDTO dto = new FacturaVentaDTO().esCredito(true).fechaEmision(LocalDate.of(2026, 1, 10));

        assertThrows(BusinessException.class, () -> service.create(dto));
        verify(repository, never()).save(any(FacturaVenta.class));
    }

    @Test
    void vencimientoAnteriorALaEmisionEsRechazado() {
        FacturaVentaDTO dto = new FacturaVentaDTO()
                .fechaEmision(LocalDate.of(2026, 2, 10))
                .fechaVencimiento(LocalDate.of(2026, 2, 1));

        assertThrows(BusinessException.class, () -> service.create(dto));
    }

    @Test
    void importesNegativosSonRechazados() {
        FacturaVentaDTO dto = new FacturaVentaDTO().importeTotal(-1.0);

        assertThrows(BusinessException.class, () -> service.update(1L, dto));
    }

    @Test
    void facturaValidaSeGuarda() {
        FacturaVentaDTO dto = new FacturaVentaDTO()
                .esCredito(true)
                .fechaEmision(LocalDate.of(2026, 2, 1))
                .fechaVencimiento(LocalDate.of(2026, 3, 1))
                .importeTotal(100.0);
        FacturaVenta entity = new FacturaVenta();
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.create(dto));
    }
}
