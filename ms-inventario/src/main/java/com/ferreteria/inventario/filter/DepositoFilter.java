package com.ferreteria.inventario.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.Inventario.Deposito;
import com.ferreteria.inventario.service.SpecificationFilter;

/** Criterios de busqueda de Deposito. Los campos nulos se ignoran. */
@Data
public class DepositoFilter implements SpecificationFilter<Deposito> {

    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String nombre;

    @Override
    public Specification<Deposito> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
