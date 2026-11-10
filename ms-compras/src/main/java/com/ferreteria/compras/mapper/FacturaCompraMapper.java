package com.ferreteria.compras.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ferreteria.commons.dto.FacturaCompraDTO;
import com.ferreteria.entity.compras.FacturaCompra;
import com.ferreteria.entity.compras.Proveedor;
import com.ferreteria.entity.general.Estado;

/**
 * Entity (relaciones @ManyToOne) <-> DTO (solo ids; Double <-> BigDecimal lo resuelve MapStruct).
 * <ul>
 *     <li>{@code importeTotal} no se acepta del cliente: lo calcula el servicio a partir de los detalles.</li>
 *     <li>En una modificacion, si el DTO no trae {@code idEstado} se conserva el estado actual
 *         (evita "reactivar" por omision una factura anulada).</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface FacturaCompraMapper extends GenericMapper<FacturaCompra, FacturaCompraDTO> {

    @Override
    @Mapping(source = "proveedor.idPersona", target = "idProveedor")
    @Mapping(source = "estado.idEstado", target = "idEstado")
    FacturaCompraDTO toDto(FacturaCompra entity);

    @Override
    @Mapping(target = "idFacturaCompra", ignore = true)
    @Mapping(target = "importeTotal", ignore = true)
    @Mapping(source = "idProveedor", target = "proveedor")
    @Mapping(source = "idEstado", target = "estado")
    FacturaCompra toEntity(FacturaCompraDTO dto);

    @Override
    @Mapping(target = "idFacturaCompra", ignore = true)
    @Mapping(target = "importeTotal", ignore = true)
    @Mapping(source = "idProveedor", target = "proveedor")
    @Mapping(source = "idEstado", target = "estado", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(FacturaCompraDTO dto, @MappingTarget FacturaCompra entity);

    default Proveedor proveedorFromId(Long id) {
        if (id == null) {
            return null;
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setIdPersona(id);
        return proveedor;
    }

    default Estado estadoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Estado estado = new Estado();
        estado.setIdEstado(id);
        return estado;
    }
}
