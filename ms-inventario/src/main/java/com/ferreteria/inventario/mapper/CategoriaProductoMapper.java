package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.CategoriaProductoDTO;
import com.ferreteria.entity.general.CategoriaProducto;

/** La coleccion "productos" de la entidad no forma parte del DTO. */
@Mapper(componentModel = "spring")
public interface CategoriaProductoMapper extends GenericMapper<CategoriaProducto, CategoriaProductoDTO> {

    @Override
    @Mapping(target = "idCategoria", ignore = true)
    @Mapping(target = "productos", ignore = true)
    CategoriaProducto toEntity(CategoriaProductoDTO dto);

    @Override
    @Mapping(target = "idCategoria", ignore = true)
    @Mapping(target = "productos", ignore = true)
    void updateEntityFromDto(CategoriaProductoDTO dto, @MappingTarget CategoriaProducto entity);
}
