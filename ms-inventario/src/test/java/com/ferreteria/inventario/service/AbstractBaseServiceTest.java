package com.ferreteria.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.entity.Inventario.Detalle_transferencia;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.exception.ResourceNotFoundException;
import com.ferreteria.inventario.mapper.DetalleTransferenciaMapper;
import com.ferreteria.inventario.repository.DetalleTransferenciaRepository;
import com.ferreteria.inventario.service.impl.DetalleTransferenciaServiceImpl;

/** Prueba la logica comun de AbstractBaseService a traves de una implementacion real. */
@ExtendWith(MockitoExtension.class)
class AbstractBaseServiceTest {

    @Mock
    private DetalleTransferenciaRepository repository;
    @Mock
    private DetalleTransferenciaMapper mapper;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private DetalleTransferenciaServiceImpl service;

    @Test
    void findAllDevuelveUnaPaginaDeDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Detalle_transferencia entity = new Detalle_transferencia();
        DetalleTransferenciaDTO dto = new DetalleTransferenciaDTO();
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<DetalleTransferenciaDTO> page = service.findAll(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(dto, page.getContent().get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPoneCadaDtoDeLaPaginaEnLaCache() {
        Pageable pageable = PageRequest.of(0, 10);
        Detalle_transferencia e1 = new Detalle_transferencia();
        Detalle_transferencia e2 = new Detalle_transferencia();
        DetalleTransferenciaDTO d1 = new DetalleTransferenciaDTO();
        d1.setIdDetTrans(1L);
        DetalleTransferenciaDTO d2 = new DetalleTransferenciaDTO();
        d2.setIdDetTrans(2L);
        Cache cache = org.mockito.Mockito.mock(Cache.class);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));
        when(mapper.toDto(e1)).thenReturn(d1);
        when(mapper.toDto(e2)).thenReturn(d2);
        when(cacheManager.getCache(CacheNames.DETALLES_TRANSFERENCIA)).thenReturn(cache);

        service.search(null, pageable);

        verify(cache).put(1L, d1);
        verify(cache).put(2L, d2);
        verify(cache, times(2)).put(any(), any());
    }

    @Test
    void findByIdInexistenteLanzaResourceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createGuardaYDevuelveElDto() {
        DetalleTransferenciaDTO dto = new DetalleTransferenciaDTO();
        Detalle_transferencia entity = new Detalle_transferencia();
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.create(dto));
        verify(repository).save(entity);
    }

    @Test
    void deleteEliminaElRegistroExistente() {
        Detalle_transferencia entity = new Detalle_transferencia();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void deleteDeInexistenteLanzaResourceNotFoundYNoBorra() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).delete(any(Detalle_transferencia.class));
    }
}
