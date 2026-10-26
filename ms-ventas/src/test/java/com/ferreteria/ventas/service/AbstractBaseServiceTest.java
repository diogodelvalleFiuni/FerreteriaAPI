package com.ferreteria.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ferreteria.commons.dto.PresupuestoDetalleDTO;
import com.ferreteria.entity.ventas.PresupuestoDetalle;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.exception.ResourceNotFoundException;
import com.ferreteria.ventas.mapper.PresupuestoDetalleMapper;
import com.ferreteria.ventas.repository.PresupuestoDetalleRepository;
import com.ferreteria.ventas.service.impl.PresupuestoDetalleServiceImpl;

/** Prueba la logica comun de AbstractBaseService (borrado fisico) a traves de una implementacion real. */
@ExtendWith(MockitoExtension.class)
class AbstractBaseServiceTest {

    @Mock
    private PresupuestoDetalleRepository repository;
    @Mock
    private PresupuestoDetalleMapper mapper;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private PresupuestoDetalleServiceImpl service;

    @Test
    void findAllDevuelveUnaPaginaDeDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        PresupuestoDetalle entity = new PresupuestoDetalle();
        PresupuestoDetalleDTO dto = new PresupuestoDetalleDTO();
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<PresupuestoDetalleDTO> page = service.findAll(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(dto, page.getContent().get(0));
    }

    @Test
    void findAllPoneCadaDtoEnLaCachePorSeparado() {
        Pageable pageable = PageRequest.of(0, 10);
        PresupuestoDetalle entity = new PresupuestoDetalle();
        PresupuestoDetalleDTO dto = new PresupuestoDetalleDTO().idPresupuestoDetalle(5L);
        Cache cache = mock(Cache.class);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);
        when(cacheManager.getCache(CacheNames.PRESUPUESTOS_DETALLE)).thenReturn(cache);

        service.findAll(pageable);

        verify(cache).put(5L, dto);
    }

    @Test
    void unaCacheCaidaNoRompeElListado() {
        Pageable pageable = PageRequest.of(0, 10);
        PresupuestoDetalle entity = new PresupuestoDetalle();
        PresupuestoDetalleDTO dto = new PresupuestoDetalleDTO().idPresupuestoDetalle(5L);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);
        when(cacheManager.getCache(CacheNames.PRESUPUESTOS_DETALLE)).thenThrow(new IllegalStateException("redis caido"));

        assertEquals(1, service.findAll(pageable).getTotalElements());
    }

    @Test
    void findByIdInexistenteLanzaResourceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createGuardaYDevuelveElDto() {
        PresupuestoDetalleDTO dto = new PresupuestoDetalleDTO();
        PresupuestoDetalle entity = new PresupuestoDetalle();
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.create(dto));
        verify(repository).save(entity);
    }

    @Test
    void deleteEliminaElRegistroExistente() {
        PresupuestoDetalle entity = new PresupuestoDetalle();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void deleteDeInexistenteLanzaResourceNotFoundYNoBorra() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).delete(any(PresupuestoDetalle.class));
    }
}
