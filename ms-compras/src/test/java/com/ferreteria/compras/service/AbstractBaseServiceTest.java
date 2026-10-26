package com.ferreteria.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ferreteria.commons.dto.ProveedorDTO;
import com.ferreteria.compras.exception.DuplicateResourceException;
import com.ferreteria.compras.exception.ResourceNotFoundException;
import com.ferreteria.compras.mapper.ProveedorMapper;
import com.ferreteria.compras.repository.ProveedorRepository;
import com.ferreteria.compras.service.impl.ProveedorServiceImpl;
import com.ferreteria.entity.compras.Proveedor;

/** Prueba la logica comun de AbstractBaseService a traves de una implementacion real. */
@ExtendWith(MockitoExtension.class)
class AbstractBaseServiceTest {

    @Mock
    private ProveedorRepository repository;
    @Mock
    private ProveedorMapper mapper;
    @Mock
    private org.springframework.cache.CacheManager cacheManager;
    @InjectMocks
    private ProveedorServiceImpl service;

    @Test
    void findAllDevuelveUnaPaginaDeDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Proveedor entity = new Proveedor();
        ProveedorDTO dto = new ProveedorDTO();
        when(repository.findAll(ArgumentMatchers.<Specification<Proveedor>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDto(entity)).thenReturn(dto);

        Page<ProveedorDTO> page = service.findAll(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(dto, page.getContent().get(0));
    }

    @Test
    void findByIdInexistenteLanzaResourceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createGuardaYDevuelveElDto() {
        ProveedorDTO dto = new ProveedorDTO();
        Proveedor entity = new Proveedor();
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.create(dto));
        verify(repository).save(entity);
    }

    @Test
    void createConRucRepetidoLanzaDuplicateYNoGuarda() {
        ProveedorDTO dto = new ProveedorDTO();
        Proveedor entity = new Proveedor();
        entity.setRuc("80012345-6");
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.existsByRucAndIdPersonaNot("80012345-6", -1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(dto));
        verify(repository, never()).save(any(Proveedor.class));
    }

    @Test
    void deleteEliminaElRegistroExistente() {
        Proveedor entity = new Proveedor();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void deleteDeInexistenteLanzaResourceNotFoundYNoBorra() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).delete(any(Proveedor.class));
    }
}
