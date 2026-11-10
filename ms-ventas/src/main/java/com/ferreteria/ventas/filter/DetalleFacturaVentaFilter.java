package com.ferreteria.ventas.filter;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.ventas.DetalleFacturaVenta;
import com.ferreteria.ventas.service.SpecificationFilter;

/** Criterios de busqueda de DetalleFacturaVenta (p. ej. todos los detalles de una factura). */
@Data
public class DetalleFacturaVentaFilter implements SpecificationFilter<DetalleFacturaVenta> {

    private Long idFactura;
    private Long idProducto;

    @Override
    public Specification<DetalleFacturaVenta> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idFactura != null) {
                predicates.add(cb.equal(root.get("factura").get("idFactura"), idFactura));
            }
            if (idProducto != null) {
                predicates.add(cb.equal(root.get("producto").get("idProducto"), idProducto));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
