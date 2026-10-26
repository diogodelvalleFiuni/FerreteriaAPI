package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.DetalleTransferenciaDTO;
import com.ferreteria.entity.Inventario.Detalle_transferencia;
import com.ferreteria.entity.Inventario.Transferencia;
import com.ferreteria.entity.general.Producto;

/**
 * Detalle_transferencia es la entidad asociativa del muchos-a-muchos Transferencia <-> Producto
 * (con el atributo propio "cantidad"): dos @ManyToOne en la entidad, dos ids en el DTO.
 */
@Mapper(componentModel = "spring")
public interface DetalleTransferenciaMapper extends GenericMapper<Detalle_transferencia, DetalleTransferenciaDTO> {

    @Override
    @Mapping(source = "transferencia.idTransferencia", target = "idTransferencia")
    @Mapping(source = "producto.idProducto", target = "idProducto")
    DetalleTransferenciaDTO toDto(Detalle_transferencia entity);

    @Override
    @Mapping(target = "idDetTrans", ignore = true)
    @Mapping(source = "idTransferencia", target = "transferencia")
    @Mapping(source = "idProducto", target = "producto")
    Detalle_transferencia toEntity(DetalleTransferenciaDTO dto);

    @Override
    @Mapping(target = "idDetTrans", ignore = true)
    @Mapping(source = "idTransferencia", target = "transferencia")
    @Mapping(source = "idProducto", target = "producto")
    void updateEntityFromDto(DetalleTransferenciaDTO dto, @MappingTarget Detalle_transferencia entity);

    default Transferencia transferenciaFromId(Long id) {
        if (id == null) {
            return null;
        }
        Transferencia transferencia = new Transferencia();
        transferencia.setIdTransferencia(id);
        return transferencia;
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
