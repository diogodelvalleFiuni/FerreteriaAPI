package com.ferreteria.inventario.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.Inventario.Inventario;
import com.ferreteria.inventario.service.SpecificationFilter;

/** Criterios de busqueda de Inventario. Los campos nulos se ignoran. */
@Data
public class InventarioFilter implements SpecificationFilter<Inventario> {

    private Long idDeposito;
    private Long idProducto;
    /** Si es true, solo los registros con cantidad menor o igual al stock minimo. */
    private Boolean bajoStockMinimo;

    @Override
    public Specification<Inventario> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idDeposito != null) {
                predicates.add(cb.equal(root.get("deposito").get("idDeposito"), idDeposito));
            }
            if (idProducto != null) {
                predicates.add(cb.equal(root.get("producto").get("idProducto"), idProducto));
            }
            if (Boolean.TRUE.equals(bajoStockMinimo)) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("cantidad"), root.<BigDecimal>get("stockMinimo")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
