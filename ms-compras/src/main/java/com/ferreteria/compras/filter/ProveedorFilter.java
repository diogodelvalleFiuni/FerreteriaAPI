package com.ferreteria.compras.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.compras.service.SpecificationFilter;
import com.ferreteria.entity.compras.Proveedor;

/** Criterios de busqueda de Proveedor. Los campos nulos se ignoran; los textos coinciden parcialmente sin distinguir mayusculas. */
@Data
public class ProveedorFilter implements SpecificationFilter<Proveedor> {

    private String nombre;
    private String apellido;
    private String ruc;
    private String correo;
    private String tipoPersona;
    private String plazoEntrega;

    @Override
    public Specification<Proveedor> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            like(predicates, cb, root, "nombre", nombre);
            like(predicates, cb, root, "apellido", apellido);
            like(predicates, cb, root, "ruc", ruc);
            like(predicates, cb, root, "correo", correo);
            like(predicates, cb, root, "tipoPersona", tipoPersona);
            like(predicates, cb, root, "plazoEntrega", plazoEntrega);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void like(List<Predicate> predicates, CriteriaBuilder cb, Root<Proveedor> root,
                             String attribute, String value) {
        if (value != null && !value.isBlank()) {
            Expression<String> path = root.get(attribute);
            predicates.add(cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
        }
    }
}
