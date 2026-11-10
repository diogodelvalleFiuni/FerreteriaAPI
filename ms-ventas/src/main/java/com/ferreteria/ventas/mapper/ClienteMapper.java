package com.ferreteria.ventas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.entity.ventas.Cliente;

/**
 * Cliente extiende Persona (herencia JOINED): el DTO trae los datos de ambas en un solo objeto.
 * Los montos del DTO son Double y los de la entidad BigDecimal (MapStruct convierte).
 * Si no se informa saldoActual al crear, queda en 0; al actualizar, un saldo nulo no pisa el actual.
 */
@Mapper(componentModel = "spring")
public interface ClienteMapper extends GenericMapper<Cliente, ClienteDTO> {

    @Override
    ClienteDTO toDto(Cliente entity);

    @Override
    @Mapping(target = "idPersona", ignore = true)
    @Mapping(target = "saldoActual", source = "saldoActual", defaultValue = "0")
    Cliente toEntity(ClienteDTO dto);

    @Override
    @Mapping(target = "idPersona", ignore = true)
    @Mapping(target = "saldoActual", source = "saldoActual",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
}
