package com.ferreteria.compras.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.DetalleFacturaCompraDTO;
import com.ferreteria.entity.compras.DetalleFacturaCompra;
import com.ferreteria.entity.compras.FacturaCompra;
import com.ferreteria.entity.general.Producto;

/**
 * DetalleFacturaCompra es la entidad asociativa del muchos-a-muchos FacturaCompra <-> Producto
 * (con atributos propios: cantidad, precio, IVA, subtotal): dos @ManyToOne en la entidad, dos ids en el DTO.
 */
@Mapper(componentModel = "spring")
public interface DetalleFacturaCompraMapper extends GenericMapper<DetalleFacturaCompra, DetalleFacturaCompraDTO> {

    @Override
    @Mapping(source = "facturaCompra.idFacturaCompra", target = "idFacturaCompra")
    @Mapping(source = "producto.idProducto", target = "idProducto")
    DetalleFacturaCompraDTO toDto(DetalleFacturaCompra entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idFacturaCompra", target = "facturaCompra")
    @Mapping(source = "idProducto", target = "producto")
    DetalleFacturaCompra toEntity(DetalleFacturaCompraDTO dto);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idFacturaCompra", target = "facturaCompra")
    @Mapping(source = "idProducto", target = "producto")
    void updateEntityFromDto(DetalleFacturaCompraDTO dto, @MappingTarget DetalleFacturaCompra entity);

    default FacturaCompra facturaFromId(Long id) {
        if (id == null) {
            return null;
        }
        FacturaCompra factura = new FacturaCompra();
        factura.setIdFacturaCompra(id);
        return factura;
    }

    default Producto productoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setIdProducto(id);
        return producto;
    }
}
