package com.ferreteria.ventas.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.ventas.PresupuestoDetalle;
import com.ferreteria.ventas.service.SpecificationFilter;

/** Criterios de busqueda de PresupuestoDetalle (p. ej. todos los detalles de un presupuesto). */
@Data
public class PresupuestoDetalleFilter implements SpecificationFilter<PresupuestoDetalle> {

    private Long idPresupuesto;
    private Long idProducto;

    @Override
    public Specification<PresupuestoDetalle> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idPresupuesto != null) {
                predicates.add(cb.equal(root.get("presupuesto").get("idPresupuesto"), idPresupuesto));
            }
            if (idProducto != null) {
                predicates.add(cb.equal(root.get("producto").get("idProducto"), idProducto));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
