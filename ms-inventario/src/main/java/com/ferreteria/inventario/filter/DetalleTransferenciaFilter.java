package com.ferreteria.inventario.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.Inventario.Detalle_transferencia;
import com.ferreteria.inventario.service.SpecificationFilter;

/** Criterios de busqueda de Detalle_transferencia. Los campos nulos se ignoran. */
@Data
public class DetalleTransferenciaFilter implements SpecificationFilter<Detalle_transferencia> {

    private Long idTransferencia;
    private Long idProducto;

    @Override
    public Specification<Detalle_transferencia> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idTransferencia != null) {
                predicates.add(cb.equal(root.get("transferencia").get("idTransferencia"), idTransferencia));
            }
            if (idProducto != null) {
                predicates.add(cb.equal(root.get("producto").get("idProducto"), idProducto));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
