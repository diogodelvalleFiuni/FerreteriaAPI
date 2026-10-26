package com.ferreteria.ventas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ferreteria.commons.dto.FacturaVentaDTO;
import com.ferreteria.entity.ventas.FacturaVenta;

/**
 * Entity (relaciones @ManyToOne) <-> DTO (solo ids). Los montos del DTO son Double y los de la
 * entidad BigDecimal (MapStruct convierte). Al actualizar, un idEstado nulo no pisa el estado actual.
 */
@Mapper(componentModel = "spring")
public interface FacturaVentaMapper extends GenericMapper<FacturaVenta, FacturaVentaDTO>, EntityReferenceMapper {

    @Override
    @Mapping(source = "cliente.idPersona", target = "idCliente")
    @Mapping(source = "presupuesto.idPresupuesto", target = "idPresupuesto")
    @Mapping(source = "estado.idEstado", target = "idEstado")
    FacturaVentaDTO toDto(FacturaVenta entity);

    @Override
    @Mapping(target = "idFactura", ignore = true)
    @Mapping(source = "idCliente", target = "cliente")
    @Mapping(source = "idPresupuesto", target = "presupuesto")
    @Mapping(source = "idEstado", target = "estado")
    FacturaVenta toEntity(FacturaVentaDTO dto);

    @Override
    @Mapping(target = "idFactura", ignore = true)
    @Mapping(source = "idCliente", target = "cliente")
    @Mapping(source = "idPresupuesto", target = "presupuesto")
    @Mapping(source = "idEstado", target = "estado",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(FacturaVentaDTO dto, @MappingTarget FacturaVenta entity);
}
