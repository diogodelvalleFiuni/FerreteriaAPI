package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.InventarioDTO;
import com.ferreteria.entity.Inventario.Deposito;
import com.ferreteria.entity.Inventario.Inventario;
import com.ferreteria.entity.general.Producto;

/**
 * Entity (relaciones @ManyToOne) <-> DTO (solo ids).
 * Al convertir DTO -> entity se crea una referencia con el id (sin consultar la BD);
 * si el id no existe, la FK lo rechaza y el GlobalExceptionHandler responde 409.
 */
@Mapper(componentModel = "spring")
public interface InventarioMapper extends GenericMapper<Inventario, InventarioDTO> {

    @Override
    @Mapping(source = "deposito.idDeposito", target = "idDeposito")
    @Mapping(source = "producto.idProducto", target = "idProducto")
    InventarioDTO toDto(Inventario entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idDeposito", target = "deposito")
    @Mapping(source = "idProducto", target = "producto")
    Inventario toEntity(InventarioDTO dto);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "idDeposito", target = "deposito")
    @Mapping(source = "idProducto", target = "producto")
    void updateEntityFromDto(InventarioDTO dto, @MappingTarget Inventario entity);

    default Deposito depositoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Deposito deposito = new Deposito();
        deposito.setIdDeposito(id);
        return deposito;
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
