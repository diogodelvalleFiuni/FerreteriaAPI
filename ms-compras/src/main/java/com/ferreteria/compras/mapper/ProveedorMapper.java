package com.ferreteria.compras.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.ProveedorDTO;
import com.ferreteria.entity.compras.Proveedor;

/** Proveedor extiende Persona (herencia JOINED): los campos heredados se mapean por nombre. */
@Mapper(componentModel = "spring")
public interface ProveedorMapper extends GenericMapper<Proveedor, ProveedorDTO> {

    @Override
    @Mapping(target = "idPersona", ignore = true)
    Proveedor toEntity(ProveedorDTO dto);

    @Override
    @Mapping(target = "idPersona", ignore = true)
    void updateEntityFromDto(ProveedorDTO dto, @MappingTarget Proveedor entity);
}
