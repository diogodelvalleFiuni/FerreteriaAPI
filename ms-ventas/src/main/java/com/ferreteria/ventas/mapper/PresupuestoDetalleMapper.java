package com.ferreteria.ventas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.PresupuestoDetalleDTO;
import com.ferreteria.entity.ventas.PresupuestoDetalle;

/**
 * PresupuestoDetalle es la entidad asociativa del muchos-a-muchos Presupuesto <-> Producto
 * (con los atributos propios cantidad y precioUnitario): dos @ManyToOne en la entidad, dos ids en el DTO.
 */
@Mapper(componentModel = "spring")
public interface PresupuestoDetalleMapper extends GenericMapper<PresupuestoDetalle, PresupuestoDetalleDTO>, EntityReferenceMapper {

    @Override
    @Mapping(source = "presupuesto.idPresupuesto", target = "idPresupuesto")
    @Mapping(source = "producto.idProducto", target = "idProducto")
    PresupuestoDetalleDTO toDto(PresupuestoDetalle entity);

    @Override
    @Mapping(target = "idPresupuestoDetalle", ignore = true)
    @Mapping(source = "idPresupuesto", target = "presupuesto")
    @Mapping(source = "idProducto", target = "producto")
    PresupuestoDetalle toEntity(PresupuestoDetalleDTO dto);

    @Override
    @Mapping(target = "idPresupuestoDetalle", ignore = true)
    @Mapping(source = "idPresupuesto", target = "presupuesto")
    @Mapping(source = "idProducto", target = "producto")
    void updateEntityFromDto(PresupuestoDetalleDTO dto, @MappingTarget PresupuestoDetalle entity);
}
