package com.ferreteria.inventario.service;

import java.math.BigDecimal;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.inventario.filter.InventarioFilter;

/**
 * Operaciones de negocio de Inventario. La implementacion esta en service/impl.
 * <p>
 * Cada metodo de negocio declara una propagacion transaccional distinta (ver la implementacion):
 * REQUIRED (subir/descontar), REQUIRES_NEW (auditoria), MANDATORY (validacion), SUPPORTS (consulta),
 * NOT_SUPPORTED (notificacion) y NEVER (purga).
 */
public interface InventarioService extends CrudService<InventarioDTO, InventarioFilter, Long> {

    /** Suma stock. Si el producto no tenia fila en el deposito, la crea. REQUIRED. */
    InventarioDTO incrementarStock(Long idDeposito, Long idProducto, BigDecimal cantidad);

    /** Resta stock. Falla (422) si no hay stock suficiente. REQUIRED. */
    InventarioDTO descontarStock(Long idDeposito, Long idProducto, BigDecimal cantidad);

    /** Stock actual (0 si no hay fila). SUPPORTS: usa la transaccion si existe, si no corre sin ella. */
    BigDecimal consultarStock(Long idDeposito, Long idProducto);

    /** Verifica que el deposito exista. MANDATORY: lanza error si se llama fuera de una transaccion. */
    void validarDepositoHabilitado(Long idDeposito);

    /** Deja constancia de una operacion en una transaccion propia. REQUIRES_NEW. */
    void registrarAuditoria(String operacion, String usuario, String detalle);

    /** Simula avisar que una transferencia comenzo. NOT_SUPPORTED: suspende la transaccion actual. */
    void notificarInicioTransferencia(Long idDepositoOrigen, Long idDepositoDestino);

    /** Simula una purga de datos historicos. NEVER: falla si hay una transaccion activa. */
    void purgarHistorico();
}
