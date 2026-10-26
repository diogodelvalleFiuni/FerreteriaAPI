package com.ferreteria.ventas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ferreteria.commons.dto.PresupuestoDTO;
import com.ferreteria.entity.ventas.Presupuesto;

/**
 * Entity (relaciones @ManyToOne) <-> DTO (solo ids). Al actualizar, un idEstado nulo
 * no pisa el estado actual (evita "reactivar" o limpiar el estado sin querer).
 */
@Mapper(componentModel = "spring")
public interface PresupuestoMapper extends GenericMapper<Presupuesto, PresupuestoDTO>, EntityReferenceMapper {

    @Override
    @Mapping(source = "cliente.idPersona", target = "idCliente")
    @Mapping(source = "estado.idEstado", target = "idEstado")
    PresupuestoDTO toDto(Presupuesto entity);

    @Override
    @Mapping(target = "idPresupuesto", ignore = true)
    @Mapping(source = "idCliente", target = "cliente")
    @Mapping(source = "idEstado", target = "estado")
    Presupuesto toEntity(PresupuestoDTO dto);

    @Override
    @Mapping(target = "idPresupuesto", ignore = true)
    @Mapping(source = "idCliente", target = "cliente")
    @Mapping(source = "idEstado", target = "estado",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PresupuestoDTO dto, @MappingTarget Presupuesto entity);
}
