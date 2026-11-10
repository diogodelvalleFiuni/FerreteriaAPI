package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.DepositoDTO;
import com.ferreteria.entity.Inventario.Deposito;

/** Las colecciones de Deposito (inventarios, transferencias) no forman parte del DTO. */
@Mapper(componentModel = "spring")
public interface DepositoMapper extends GenericMapper<Deposito, DepositoDTO> {

    @Override
    @Mapping(target = "idDeposito", ignore = true)
    @Mapping(target = "inventarios", ignore = true)
    @Mapping(target = "transferenciasComoOrigen", ignore = true)
    @Mapping(target = "transferenciasComoDestino", ignore = true)
    Deposito toEntity(DepositoDTO dto);

    @Override
    @Mapping(target = "idDeposito", ignore = true)
    @Mapping(target = "inventarios", ignore = true)
    @Mapping(target = "transferenciasComoOrigen", ignore = true)
    @Mapping(target = "transferenciasComoDestino", ignore = true)
    void updateEntityFromDto(DepositoDTO dto, @MappingTarget Deposito entity);
}
