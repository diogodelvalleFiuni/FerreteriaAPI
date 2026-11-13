package com.ferreteria.inventario.service.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.entity.Inventario.Inventario;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.exception.BusinessException;
import com.ferreteria.inventario.filter.InventarioFilter;
import com.ferreteria.inventario.mapper.InventarioMapper;
import com.ferreteria.inventario.repository.DepositoRepository;
import com.ferreteria.inventario.repository.InventarioRepository;
import com.ferreteria.inventario.repository.ProductoRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.InventarioService;
import com.ferreteria.inventario.util.TxInfo;

@Service
public class InventarioServiceImpl
        extends AbstractBaseService<Inventario, InventarioDTO, InventarioFilter, Long, InventarioRepository, InventarioMapper>
        implements InventarioService {

    /** Logger aparte para poder filtrar la auditoria en los logs (nombre fijo, no depende del paquete). */
    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA_INVENTARIO");

    private final DepositoRepository depositoRepository;
    private final ProductoRepository productoRepository;

    public InventarioServiceImpl(InventarioRepository repository, InventarioMapper mapper, CacheManager cacheManager,
            DepositoRepository depositoRepository, ProductoRepository productoRepository) {
        super(repository, mapper, cacheManager);
        this.depositoRepository = depositoRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    protected String cacheName() {
        return CacheNames.INVENTARIOS;
    }

    @Override
    protected Object idOf(InventarioDTO dto) {
        return dto.getId();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public InventarioDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.INVENTARIOS, key = "#result.id")
    public InventarioDTO create(InventarioDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public InventarioDTO update(Long id, InventarioDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.INVENTARIOS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }

    // ------------------------------------------------------------------------------------------------
    // Logica de stock
    // ------------------------------------------------------------------------------------------------

    /**
     * REQUIRED: si ya hay una transaccion (ej. la de la transferencia) participa en ella; si no, abre una.
     * Asi, subir stock en el destino y descontarlo en el origen se confirman o se deshacen juntos.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public InventarioDTO incrementarStock(Long idDeposito, Long idProducto, BigDecimal cantidad) {
        validarCantidad(cantidad);
        log.debug("incrementarStock deposito={} producto={} cantidad={} {}", idDeposito, idProducto, cantidad, TxInfo.estado());

        Inventario inventario = repository.findByDeposito_IdDepositoAndProducto_IdProducto(idDeposito, idProducto)
                .orElseGet(() -> nuevoInventario(idDeposito, idProducto));

        BigDecimal nuevo = cantidadDe(inventario).add(cantidad);
        inventario.setCantidad(nuevo);
        if (esPositivo(inventario.getStockMaximo()) && nuevo.compareTo(inventario.getStockMaximo()) > 0) {
            log.warn("Stock por encima del maximo: deposito={} producto={} cantidad={} maximo={}",
                    idDeposito, idProducto, nuevo, inventario.getStockMaximo());
        }

        Inventario guardado = repository.save(inventario);
        evictAfterCommit(guardado.getId());
        return mapper.toDto(guardado);
    }

    /** REQUIRED: igual que {@link #incrementarStock}. La fila se bloquea para que dos descuentos no se pisen. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public InventarioDTO descontarStock(Long idDeposito, Long idProducto, BigDecimal cantidad) {
        validarCantidad(cantidad);
        log.debug("descontarStock deposito={} producto={} cantidad={} {}", idDeposito, idProducto, cantidad, TxInfo.estado());

        Inventario inventario = repository.findByDeposito_IdDepositoAndProducto_IdProducto(idDeposito, idProducto)
                .orElseThrow(() -> new BusinessException(
                        "El producto " + idProducto + " no tiene stock en el deposito " + idDeposito));

        BigDecimal actual = cantidadDe(inventario);
        if (actual.compareTo(cantidad) < 0) {
            throw new BusinessException("Stock insuficiente del producto " + idProducto + " en el deposito "
                    + idDeposito + ": disponible " + actual + ", solicitado " + cantidad);
        }

        BigDecimal nuevo = actual.subtract(cantidad);
        inventario.setCantidad(nuevo);
        if (nuevo.compareTo(cantidadDe(inventario.getStockMinimo())) <= 0) {
            log.warn("Stock en o bajo el minimo: deposito={} producto={} cantidad={} minimo={}",
                    idDeposito, idProducto, nuevo, inventario.getStockMinimo());
        }

        Inventario guardado = repository.save(inventario);
        evictAfterCommit(guardado.getId());
        return mapper.toDto(guardado);
    }

    /**
     * SUPPORTS: dentro de la transferencia usa su misma transaccion/conexion; llamado desde una consulta web
     * simple no abre ninguna.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public BigDecimal consultarStock(Long idDeposito, Long idProducto) {
        log.debug("consultarStock deposito={} producto={} {}", idDeposito, idProducto, TxInfo.estado());
        return repository.findFirstByDeposito_IdDepositoAndProducto_IdProducto(idDeposito, idProducto)
                .map(this::cantidadDe)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * MANDATORY: exige una transaccion ya abierta; si alguien lo llama suelto, Spring lanza
     * IllegalTransactionStateException. Las entidades de commons no tienen columna de estado, asi que
     * "habilitado" significa que el deposito exista.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void validarDepositoHabilitado(Long idDeposito) {
        log.debug("validarDepositoHabilitado deposito={} {}", idDeposito, TxInfo.estado());
        if (idDeposito == null || !depositoRepository.existsById(idDeposito)) {
            throw new BusinessException("El deposito " + idDeposito + " no existe o no esta habilitado");
        }
    }

    /**
     * REQUIRES_NEW: suspende la transaccion actual y abre una propia, que se confirma aunque la principal luego
     * falle. Hoy solo escribe en el log (el esquema lo administra commons y no tiene tabla de auditoria); si se agrega
     * la tabla, aca se guardaria la fila y asi quedaria independiente del rollback de la transferencia.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAuditoria(String operacion, String usuario, String detalle) {
        AUDITORIA.info("AUDITORIA operacion={} usuario={} detalle={} {}", operacion, usuario, detalle, TxInfo.estado());
    }

    /** NOT_SUPPORTED: suspende la transaccion actual (y su conexion queda libre) mientras corre; luego la reanuda. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notificarInicioTransferencia(Long idDepositoOrigen, Long idDepositoDestino) {
        log.info("Notificacion simulada: transferencia iniciada del deposito {} al {} {}",
                idDepositoOrigen, idDepositoDestino, TxInfo.estado());
    }

    /** NEVER: si hay una transaccion activa lanza IllegalTransactionStateException; no la suspende como NOT_SUPPORTED. */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void purgarHistorico() {
        log.info("Purga simulada de datos historicos {}", TxInfo.estado());
    }

    // ------------------------------------------------------------------------------------------------

    private Inventario nuevoInventario(Long idDeposito, Long idProducto) {
        if (!depositoRepository.existsById(idDeposito)) {
            throw new BusinessException("El deposito " + idDeposito + " no existe");
        }
        if (!productoRepository.existsById(idProducto)) {
            throw new BusinessException("El producto " + idProducto + " no existe");
        }
        log.debug("Creando fila de inventario deposito={} producto={}", idDeposito, idProducto);
        Inventario inventario = new Inventario();
        inventario.setDeposito(mapper.depositoFromId(idDeposito));
        inventario.setProducto(mapper.productoFromId(idProducto));
        inventario.setCantidad(BigDecimal.ZERO);
        inventario.setStockMinimo(BigDecimal.ZERO);
        inventario.setStockMaximo(BigDecimal.ZERO);
        return inventario;
    }

    private void validarCantidad(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new BusinessException("La cantidad debe ser mayor a cero");
        }
    }

    private BigDecimal cantidadDe(Inventario inventario) {
        return cantidadDe(inventario.getCantidad());
    }

    private BigDecimal cantidadDe(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private boolean esPositivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /**
     * Invalida la entrada de Redis recien DESPUES del commit: si se hiciera antes, una lectura concurrente podria
     * volver a cachear el valor viejo mientras la transaccion todavia no termino.
     */
    private void evictAfterCommit(Long id) {
        if (id == null) {
            return;
        }
        Runnable evict = () -> {
            try {
                Cache cache = cacheManager.getCache(cacheName());
                if (cache != null) {
                    cache.evict(id);
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo invalidar la cache '{}' para {}: {}", cacheName(), id, ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }
}
