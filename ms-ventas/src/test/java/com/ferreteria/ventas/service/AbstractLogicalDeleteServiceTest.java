package com.ferreteria.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.entity.general.Estado;
import com.ferreteria.entity.ventas.Presupuesto;
import com.ferreteria.ventas.exception.ResourceNotFoundException;
import com.ferreteria.ventas.filter.PresupuestoFilter;
import com.ferreteria.ventas.mapper.PresupuestoMapper;
import com.ferreteria.ventas.repository.PresupuestoRepository;
import com.ferreteria.ventas.service.impl.PresupuestoServiceImpl;

/** Prueba el borrado logico de AbstractLogicalDeleteService a traves de PresupuestoServiceImpl. */
@ExtendWith(MockitoExtension.class)
class AbstractLogicalDeleteServiceTest {

    private static final Long ESTADO_INACTIVO = 9L;

    @Mock
    private PresupuestoRepository repository;
    @Mock
    private PresupuestoMapper mapper;
    @Mock
    private EstadoService estadoService;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private PresupuestoServiceImpl service;

    private static Estado estado(Long id) {
        Estado estado = new Estado();
        estado.setIdEstado(id);
        return estado;
    }

    private static Presupuesto presupuestoConEstado(Long idEstado) {
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setEstado(estado(idEstado));
        return presupuesto;
    }

    @Test
    void deleteMarcaElEstadoInactivoYNoBorraLaFila() {
        Presupuesto entity = presupuestoConEstado(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(estadoService.getInactivoId()).thenReturn(ESTADO_INACTIVO);
        when(estadoService.getInactivo()).thenReturn(estado(ESTADO_INACTIVO));

        service.delete(1L);

        assertEquals(ESTADO_INACTIVO, entity.getEstado().getIdEstado());
        verify(repository).save(entity);
        verify(repository, never()).delete(any(Presupuesto.class));
    }

    @Test
    void deleteDeUnRegistroYaInactivoLanzaResourceNotFound() {
        when(repository.findById(2L)).thenReturn(Optional.of(presupuestoConEstado(ESTADO_INACTIVO)));
        when(estadoService.getInactivoId()).thenReturn(ESTADO_INACTIVO);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(2L));
        verify(repository, never()).save(any(Presupuesto.class));
    }

    @Test
    void findByIdDeUnRegistroInactivoLanzaResourceNotFound() {
        when(repository.findById(3L)).thenReturn(Optional.of(presupuestoConEstado(ESTADO_INACTIVO)));
        when(estadoService.getInactivoId()).thenReturn(ESTADO_INACTIVO);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(3L));
    }

    @Test
    void findByIdDeUnRegistroSinEstadoSeConsideraActivo() {
        Presupuesto entity = new Presupuesto();
        PresupuestoDTO dto = new PresupuestoDTO();
        when(repository.findById(4L)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.findById(4L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllYSearchConsultanSoloRegistrosActivos() {
        Pageable pageable = PageRequest.of(0, 10);
        Presupuesto entity = new Presupuesto();
        PresupuestoDTO dto = new PresupuestoDTO();
        when(estadoService.getInactivoId()).thenReturn(ESTADO_INACTIVO);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<PresupuestoDTO> lista = service.findAll(pageable);
        Page<PresupuestoDTO> busqueda = service.search(new PresupuestoFilter(), pageable);

        assertEquals(dto, lista.getContent().get(0));
        assertEquals(dto, busqueda.getContent().get(0));
        // ambas consultas usan Specification (condicion "activo"), nunca findAll(pageable) a secas
        verify(repository, never()).findAll(any(Pageable.class));
    }
}
