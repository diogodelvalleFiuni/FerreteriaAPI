package com.ferreteria.ventas.filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.ventas.Presupuesto;
import com.ferreteria.ventas.service.SpecificationFilter;

/** Criterios de busqueda de Presupuesto. Los campos nulos se ignoran. */
@Data
public class PresupuestoFilter implements SpecificationFilter<Presupuesto> {

    private Long idCliente;
    private Long idEstado;
    /** Formato ISO yyyy-MM-dd, inclusive. */
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;

    @Override
    public Specification<Presupuesto> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idCliente != null) {
                predicates.add(cb.equal(root.get("cliente").get("idPersona"), idCliente));
            }
            if (idEstado != null) {
                predicates.add(cb.equal(root.get("estado").get("idEstado"), idEstado));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fecha"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fecha"), fechaHasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
