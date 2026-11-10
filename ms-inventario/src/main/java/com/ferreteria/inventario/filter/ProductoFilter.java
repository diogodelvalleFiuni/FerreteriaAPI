package com.ferreteria.inventario.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.general.Producto;
import com.ferreteria.inventario.service.SpecificationFilter;

/** Criterios de busqueda de Producto. Los campos nulos se ignoran. */
@Data
public class ProductoFilter implements SpecificationFilter<Producto> {

    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String nombre;
    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String codigo;
    private Long idMarca;
    private Long idCategoria;

    @Override
    public Specification<Producto> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (codigo != null && !codigo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("codigo")), "%" + codigo.toLowerCase() + "%"));
            }
            if (idMarca != null) {
                predicates.add(cb.equal(root.get("marca").get("idMarca"), idMarca));
            }
            if (idCategoria != null) {
                predicates.add(cb.equal(root.get("categoria").get("idCategoria"), idCategoria));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
