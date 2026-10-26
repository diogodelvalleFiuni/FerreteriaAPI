package com.ferreteria.inventario.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ferreteria.commons.dto.ProductoDTO;
import com.ferreteria.entity.general.CategoriaProducto;
import com.ferreteria.entity.general.Marca;
import com.ferreteria.entity.general.Producto;

/**
 * Entity (relaciones @ManyToOne a Marca y CategoriaProducto) <-> DTO (solo ids).
 * Si la marca o la categoria no existen, la FK lo rechaza y el GlobalExceptionHandler responde 409.
 */
@Mapper(componentModel = "spring")
public interface ProductoMapper extends GenericMapper<Producto, ProductoDTO> {

    @Override
    @Mapping(source = "marca.idMarca", target = "idMarca")
    @Mapping(source = "categoria.idCategoria", target = "idCategoria")
    ProductoDTO toDto(Producto entity);

    @Override
    @Mapping(target = "idProducto", ignore = true)
    @Mapping(source = "idMarca", target = "marca")
    @Mapping(source = "idCategoria", target = "categoria")
    Producto toEntity(ProductoDTO dto);

    @Override
    @Mapping(target = "idProducto", ignore = true)
    @Mapping(source = "idMarca", target = "marca")
    @Mapping(source = "idCategoria", target = "categoria")
    void updateEntityFromDto(ProductoDTO dto, @MappingTarget Producto entity);

    default Marca marcaFromId(Long id) {
        if (id == null) {
            return null;
        }
        Marca marca = new Marca();
        marca.setIdMarca(id);
        return marca;
    }

    default CategoriaProducto categoriaFromId(Long id) {
        if (id == null) {
            return null;
        }
        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setIdCategoria(id);
        return categoria;
    }
}
