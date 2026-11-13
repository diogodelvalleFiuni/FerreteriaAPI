package com.ferreteria.inventario.dto;

import java.util.List;

import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.commons.dto.TransferenciaDTO;

/** Resultado de procesar una transferencia: la cabecera guardada y sus detalles. */
public record TransferenciaProcesadaDTO(TransferenciaDTO transferencia, List<DetalleTransferenciaDTO> detalles) {
}
