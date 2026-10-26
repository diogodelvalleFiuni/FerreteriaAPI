package com.ferreteria.ventas.service.impl;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.entity.ventas.Cliente;
import com.ferreteria.ventas.config.CacheNames;
import com.ferreteria.ventas.exception.BusinessException;
import com.ferreteria.ventas.filter.ClienteFilter;
import com.ferreteria.ventas.mapper.ClienteMapper;
import com.ferreteria.ventas.repository.ClienteRepository;
import com.ferreteria.ventas.service.AbstractBaseService;
import com.ferreteria.ventas.service.ClienteService;

/**
 * La tabla clientes no tiene columna de estado, por eso el DELETE elimina la fila
 * (comportamiento por defecto de {@link AbstractBaseService}). Reglas de negocio:
 * <ul>
 *     <li>el CI no puede repetirse (409)</li>
 *     <li>el saldo actual no puede superar el limite de saldo (400)</li>
 * </ul>
 */
@Service
public class ClienteServiceImpl
        extends AbstractBaseService<Cliente, ClienteDTO, ClienteFilter, Long, ClienteRepository, ClienteMapper>
        implements ClienteService {

    public ClienteServiceImpl(ClienteRepository repository, ClienteMapper mapper, CacheManager cacheManager) {
        super(repository, mapper, cacheManager);
    }

    @Override
    protected String cacheName() {
        return CacheNames.CLIENTES;
    }

    @Override
    protected Object idOf(ClienteDTO dto) {
        return dto.getIdPersona();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CLIENTES, key = "#id")
    public ClienteDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: valida, guarda y deja el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.CLIENTES, key = "#result.idPersona")
    public ClienteDTO create(ClienteDTO dto) {
        validarSaldo(dto);
        if (dto.getCi() != null && repository.existsByCi(dto.getCi())) {
            throw ciDuplicado(dto.getCi());
        }
        return super.create(dto);
    }

    /** Actualizacion: valida e invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CLIENTES, key = "#id")
    public ClienteDTO update(Long id, ClienteDTO dto) {
        validarSaldo(dto);
        if (dto.getCi() != null && repository.existsByCiAndIdPersonaNot(dto.getCi(), id)) {
            throw ciDuplicado(dto.getCi());
        }
        return super.update(id, dto);
    }

    /** Borrado: invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CLIENTES, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }

    private void validarSaldo(ClienteDTO dto) {
        log.debug("Validando saldo del cliente");
        if (dto.getSaldoActual() != null && dto.getLimiteSaldo() != null
                && dto.getSaldoActual() > dto.getLimiteSaldo()) {
            throw new BusinessException("El saldo actual no puede superar el limite de saldo");
        }
    }

    private BusinessException ciDuplicado(String ci) {
        log.warn("Ya existe un cliente con CI {}", ci);
        return new BusinessException(HttpStatus.CONFLICT, "Ya existe un cliente con el CI " + ci);
    }
}
