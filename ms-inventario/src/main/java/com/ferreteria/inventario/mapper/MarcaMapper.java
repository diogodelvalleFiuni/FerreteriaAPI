package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.MarcaDTO;
import com.ferreteria.entity.general.Marca;

/** La coleccion "productos" de la entidad no forma parte del DTO. */
@Mapper(componentModel = "spring")
public interface MarcaMapper extends GenericMapper<Marca, MarcaDTO> {

    @Override
    @Mapping(target = "idMarca", ignore = true)
    @Mapping(target = "productos", ignore = true)
    Marca toEntity(MarcaDTO dto);

    @Override
    @Mapping(target = "idMarca", ignore = true)
    @Mapping(target = "productos", ignore = true)
    void updateEntityFromDto(MarcaDTO dto, @MappingTarget Marca entity);
}
