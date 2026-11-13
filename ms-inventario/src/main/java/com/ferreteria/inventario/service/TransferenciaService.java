package com.ferreteria.inventario.service;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.inventario.dto.ProcesarTransferenciaRequest;
import com.ferreteria.inventario.dto.TransferenciaProcesadaDTO;
import com.ferreteria.inventario.filter.TransferenciaFilter;

/** Operaciones de negocio de Transferencia. La implementacion esta en service/impl. */
public interface TransferenciaService extends CrudService<TransferenciaDTO, TransferenciaFilter, Long> {

    /**
     * Procesa una transferencia completa en una sola transaccion (REQUIRED): guarda la cabecera, descuenta cada
     * producto del deposito de origen, lo suma en el destino y guarda los detalles. Si algo falla, se deshace todo.
     *
     * @param creador usuario autenticado (claim "sub" del token)
     */
    TransferenciaProcesadaDTO procesarTransferencia(ProcesarTransferenciaRequest request, String creador);
}
