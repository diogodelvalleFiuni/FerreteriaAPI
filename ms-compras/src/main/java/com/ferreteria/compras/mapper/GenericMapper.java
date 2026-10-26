package com.ferreteria.compras.mapper;

import java.util.List;

import org.mapstruct.MappingTarget;

/**
 * Contrato generico de mapeo Entidad &lt;-&gt; DTO.
 * <p>
 * Cada modulo implementa esta interfaz con MapStruct, por ejemplo:
 * <pre>{@code
 * @Mapper(componentModel = "spring")
 * public interface ProductoMapper extends GenericMapper<Producto, ProductoDTO> {
 * }
 * }</pre>
 * MapStruct genera la implementacion automaticamente a partir de los
 * nombres de campos de E y D.
 *
 * @param <E> tipo de entidad
 * @param <D> tipo de DTO
 */
public interface GenericMapper<E, D> {

    D toDto(E entity);

    E toEntity(D dto);

    List<D> toDtoList(List<E> entities);

    List<E> toEntityList(List<D> dtos);

    /** Copia los datos del DTO sobre una entidad ya existente (para updates), sin pisar el id. */
    void updateEntityFromDto(D dto, @MappingTarget E entity);
}
