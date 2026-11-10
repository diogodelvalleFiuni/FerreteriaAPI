package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.TransferenciaDTO;
import com.ferreteria.entity.Inventario.Deposito;
import com.ferreteria.entity.Inventario.Transferencia;

/**
 * Entity (LocalDateTime + relaciones) <-> DTO (LocalDateTime + ids).
 * Las fechas son LocalDateTime en ambos lados (JSON ISO-8601, ej. "2026-10-24T14:30:00"),
 * asi que MapStruct las copia directo, sin conversion manual.
 */
@Mapper(componentModel = "spring")
public interface TransferenciaMapper extends GenericMapper<Transferencia, TransferenciaDTO> {

    @Override
    @Mapping(source = "depositoOrigen.idDeposito", target = "idDepositoOrigen")
    @Mapping(source = "depositoDestino.idDeposito", target = "idDepositoDestino")
    TransferenciaDTO toDto(Transferencia entity);

    @Override
    @Mapping(target = "idTransferencia", ignore = true)
    @Mapping(source = "idDepositoOrigen", target = "depositoOrigen")
    @Mapping(source = "idDepositoDestino", target = "depositoDestino")
    Transferencia toEntity(TransferenciaDTO dto);

    @Override
    @Mapping(target = "idTransferencia", ignore = true)
    @Mapping(source = "idDepositoOrigen", target = "depositoOrigen")
    @Mapping(source = "idDepositoDestino", target = "depositoDestino")
    void updateEntityFromDto(TransferenciaDTO dto, @MappingTarget Transferencia entity);

    default Deposito depositoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Deposito deposito = new Deposito();
        deposito.setIdDeposito(id);
        return deposito;
    }
}
