package com.ferreteria.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.entity.ventas.Cliente;
import com.ferreteria.ventas.exception.BusinessException;
import com.ferreteria.ventas.mapper.ClienteMapper;
import com.ferreteria.ventas.repository.ClienteRepository;
import com.ferreteria.ventas.service.impl.ClienteServiceImpl;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository repository;
    @Mock
    private ClienteMapper mapper;
    @Mock
    private CacheManager cacheManager;
    @InjectMocks
    private ClienteServiceImpl service;

    @Test
    void createConCiRepetidoDevuelveConflictoYNoGuarda() {
        ClienteDTO dto = new ClienteDTO().ci("123");
        when(repository.existsByCi("123")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void createConSaldoMayorAlLimiteEsRechazado() {
        ClienteDTO dto = new ClienteDTO().limiteSaldo(100.0).saldoActual(150.0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createValidoGuardaYDevuelveElDto() {
        ClienteDTO dto = new ClienteDTO().ci("123").limiteSaldo(100.0).saldoActual(10.0);
        Cliente entity = new Cliente();
        when(repository.existsByCi("123")).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenReturn(dto);

        assertEquals(dto, service.create(dto));
    }

    @Test
    void updateConCiDeOtroClienteEsConflicto() {
        ClienteDTO dto = new ClienteDTO().ci("123");
        when(repository.existsByCiAndIdPersonaNot("123", 7L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(7L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
