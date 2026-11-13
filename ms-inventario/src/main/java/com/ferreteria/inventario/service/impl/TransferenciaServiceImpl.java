package com.ferreteria.inventario.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.entity.Inventario.Detalle_transferencia;
import com.ferreteria.entity.Inventario.Transferencia;
import com.ferreteria.inventario.config.CacheNames;
import com.ferreteria.inventario.dto.ProcesarTransferenciaRequest;
import com.ferreteria.inventario.dto.ProcesarTransferenciaRequest.Item;
import com.ferreteria.inventario.dto.TransferenciaProcesadaDTO;
import com.ferreteria.inventario.exception.BusinessException;
import com.ferreteria.inventario.filter.TransferenciaFilter;
import com.ferreteria.inventario.mapper.DetalleTransferenciaMapper;
import com.ferreteria.inventario.mapper.TransferenciaMapper;
import com.ferreteria.inventario.repository.DetalleTransferenciaRepository;
import com.ferreteria.inventario.repository.TransferenciaRepository;
import com.ferreteria.inventario.service.AbstractBaseService;
import com.ferreteria.inventario.service.InventarioService;
import com.ferreteria.inventario.service.TransferenciaService;
import com.ferreteria.inventario.util.TxInfo;

@Service
public class TransferenciaServiceImpl
        extends AbstractBaseService<Transferencia, TransferenciaDTO, TransferenciaFilter, Long, TransferenciaRepository, TransferenciaMapper>
        implements TransferenciaService {

    private final InventarioService inventarioService;
    private final DetalleTransferenciaRepository detalleRepository;
    private final DetalleTransferenciaMapper detalleMapper;

    public TransferenciaServiceImpl(TransferenciaRepository repository, TransferenciaMapper mapper, CacheManager cacheManager,
            InventarioService inventarioService, DetalleTransferenciaRepository detalleRepository,
            DetalleTransferenciaMapper detalleMapper) {
        super(repository, mapper, cacheManager);
        this.inventarioService = inventarioService;
        this.detalleRepository = detalleRepository;
        this.detalleMapper = detalleMapper;
    }

    @Override
    protected String cacheName() {
        return CacheNames.TRANSFERENCIAS;
    }

    @Override
    protected Object idOf(TransferenciaDTO dto) {
        return dto.getIdTransferencia();
    }

    /** Lectura por id: si esta en Redis no toca la base. */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public TransferenciaDTO findById(Long id) {
        return super.findById(id);
    }

    /** Creacion: guarda el DTO resultante en la cache con su id nuevo como clave. */
    @Override
    @Transactional
    @CachePut(cacheNames = CacheNames.TRANSFERENCIAS, key = "#result.idTransferencia")
    public TransferenciaDTO create(TransferenciaDTO dto) {
        return super.create(dto);
    }

    /** Actualizacion: invalida la entrada; la proxima lectura la recarga desde la base. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public TransferenciaDTO update(Long id, TransferenciaDTO dto) {
        return super.update(id, dto);
    }

    /** Borrado: tambien invalida la entrada para no servir un registro que ya no existe. */
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.TRANSFERENCIAS, key = "#id")
    public void delete(Long id) {
        super.delete(id);
    }

    // ------------------------------------------------------------------------------------------------
    // Transferencia de stock
    // ------------------------------------------------------------------------------------------------

    /**
     * REQUIRED: es el contexto transaccional base. Todos los metodos de {@link InventarioService} que se llaman
     * desde aca cruzan el proxy de Spring, asi que respetan su propia propagacion:
     * <ul>
     *     <li>NOT_SUPPORTED (notificar): suspende esta transaccion y la reanuda</li>
     *     <li>REQUIRES_NEW (auditoria): transaccion propia, independiente del rollback de esta</li>
     *     <li>MANDATORY (validar deposito): participa en esta (fallaria si no existiera)</li>
     *     <li>SUPPORTS (consultar stock): participa en esta</li>
     *     <li>REQUIRED (descontar / incrementar): participa en esta</li>
     * </ul>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public TransferenciaProcesadaDTO procesarTransferencia(ProcesarTransferenciaRequest request, String creador) {
        Long origen = request.idDepositoOrigen();
        Long destino = request.idDepositoDestino();
        if (origen.equals(destino)) {
            throw new BusinessException("El deposito de origen y el de destino deben ser distintos");
        }
        log.info("Procesando transferencia {} -> {} ({} producto/s) {}", origen, destino, request.items().size(), TxInfo.estado());

        String resumen = "origen=" + origen + ", destino=" + destino + ", items=" + request.items().size();
        inventarioService.notificarInicioTransferencia(origen, destino);                      // NOT_SUPPORTED
        inventarioService.registrarAuditoria("TRANSFERENCIA_INICIADA", creador, resumen);     // REQUIRES_NEW

        try {
            TransferenciaProcesadaDTO resultado = ejecutarTransferencia(request, creador);
            inventarioService.registrarAuditoria("TRANSFERENCIA_COMPLETADA", creador,
                    resumen + ", id=" + resultado.transferencia().getIdTransferencia());      // REQUIRES_NEW
            return resultado;
        } catch (RuntimeException ex) {
            // La transaccion principal se deshace al relanzar; este registro (REQUIRES_NEW) queda igual.
            inventarioService.registrarAuditoria("TRANSFERENCIA_FALLIDA", creador, resumen + ", motivo=" + ex.getMessage());
            throw ex;
        }
    }

    /** Cuerpo de la transferencia; corre dentro de la transaccion de {@link #procesarTransferencia}. */
    private TransferenciaProcesadaDTO ejecutarTransferencia(ProcesarTransferenciaRequest request, String creador) {
        Long origen = request.idDepositoOrigen();
        Long destino = request.idDepositoDestino();

        inventarioService.validarDepositoHabilitado(origen);                                  // MANDATORY
        inventarioService.validarDepositoHabilitado(destino);                                 // MANDATORY

        // 1) Cabecera
        TransferenciaDTO cabecera = new TransferenciaDTO();
        cabecera.setFechaCreacion(LocalDateTime.now());
        cabecera.setFechaEstimada(request.fechaEstimada());
        cabecera.setCreador(creador);
        cabecera.setIdDepositoOrigen(origen);
        cabecera.setIdDepositoDestino(destino);
        Transferencia guardada = repository.save(mapper.toEntity(cabecera));
        log.debug("Cabecera de transferencia guardada, id={}", guardada.getIdTransferencia());

        // 2) Detalles. Se recorren ordenados por producto: dos transferencias que toquen los mismos productos
        //    bloquean las filas en el mismo orden y se reduce el riesgo de interbloqueo.
        List<Item> items = new ArrayList<>(request.items());
        items.sort(Comparator.comparing(Item::idProducto));

        List<Detalle_transferencia> detalles = new ArrayList<>();
        for (Item item : items) {
            BigDecimal disponible = inventarioService.consultarStock(origen, item.idProducto());   // SUPPORTS
            log.debug("Producto {}: disponible en origen {}, a transferir {}", item.idProducto(), disponible, item.cantidad());

            inventarioService.descontarStock(origen, item.idProducto(), item.cantidad());          // REQUIRED
            inventarioService.incrementarStock(destino, item.idProducto(), item.cantidad());       // REQUIRED

            Detalle_transferencia detalle = new Detalle_transferencia();
            detalle.setTransferencia(guardada);
            detalle.setProducto(detalleMapper.productoFromId(item.idProducto()));
            detalle.setCantidad(item.cantidad());
            detalles.add(detalleRepository.save(detalle));
        }

        return new TransferenciaProcesadaDTO(mapper.toDto(guardada), detalleMapper.toDtoList(detalles));
    }
}
