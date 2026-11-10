package com.ferreteria.compras.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.compras.service.SpecificationFilter;
import com.ferreteria.entity.compras.DetalleFacturaCompra;

/** Criterios de busqueda de DetalleFacturaCompra. Los campos nulos se ignoran. */
@Data
public class DetalleFacturaCompraFilter implements SpecificationFilter<DetalleFacturaCompra> {

    private Long idFacturaCompra;
    private Long idProducto;
    private Integer porcentajeIva;
    private Integer cantidadMin;
    private Integer cantidadMax;
    private BigDecimal subtotalMin;
    private BigDecimal subtotalMax;

    @Override
    public Specification<DetalleFacturaCompra> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idFacturaCompra != null) {
                predicates.add(cb.equal(root.get("facturaCompra").get("idFacturaCompra"), idFacturaCompra));
            }
            if (idProducto != null) {
                predicates.add(cb.equal(root.get("producto").get("idProducto"), idProducto));
            }
            if (porcentajeIva != null) {
                predicates.add(cb.equal(root.<Integer>get("porcentajeIva"), porcentajeIva));
            }
            if (cantidadMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("cantidad"), cantidadMin));
            }
            if (cantidadMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("cantidad"), cantidadMax));
            }
            if (subtotalMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("subtotal"), subtotalMin));
            }
            if (subtotalMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("subtotal"), subtotalMax));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
