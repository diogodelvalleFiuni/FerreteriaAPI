package com.ferreteria.ventas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.DetalleFacturaVentaDTO;
import com.ferreteria.entity.ventas.DetalleFacturaVenta;

/**
 * DetalleFacturaVenta es la entidad asociativa del muchos-a-muchos FacturaVenta <-> Producto
 * (con cantidad y precioUnitario, heredados de DetalleFactura): dos @ManyToOne, dos ids en el DTO.
 */
@Mapper(componentModel = "spring")
public interface DetalleFacturaVentaMapper extends GenericMapper<DetalleFacturaVenta, DetalleFacturaVentaDTO>, EntityReferenceMapper {

    @Override
    @Mapping(source = "producto.idProducto", target = "idProducto")
    @Mapping(source = "factura.idFactura", target = "idFactura")
    DetalleFacturaVentaDTO toDto(DetalleFacturaVenta entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idProducto", target = "producto")
    @Mapping(source = "idFactura", target = "factura")
    DetalleFacturaVenta toEntity(DetalleFacturaVentaDTO dto);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idProducto", target = "producto")
    @Mapping(source = "idFactura", target = "factura")
    void updateEntityFromDto(DetalleFacturaVentaDTO dto, @MappingTarget DetalleFacturaVenta entity);
}
