package com.ferreteria.ventas.filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.ventas.FacturaVenta;
import com.ferreteria.ventas.service.SpecificationFilter;

/** Criterios de busqueda de FacturaVenta. Los campos nulos se ignoran. */
@Data
public class FacturaVentaFilter implements SpecificationFilter<FacturaVenta> {

    private Long idCliente;
    private Long idPresupuesto;
    private Long idEstado;
    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String nroFactura;
    private String codigoFactura;
    private Boolean esCredito;
    /** Formato ISO yyyy-MM-dd, inclusive. */
    private LocalDate fechaEmisionDesde;
    private LocalDate fechaEmisionHasta;
    private LocalDate fechaVencimientoDesde;
    private LocalDate fechaVencimientoHasta;

    @Override
    public Specification<FacturaVenta> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idCliente != null) {
                predicates.add(cb.equal(root.get("cliente").get("idPersona"), idCliente));
            }
            if (idPresupuesto != null) {
                predicates.add(cb.equal(root.get("presupuesto").get("idPresupuesto"), idPresupuesto));
            }
            if (idEstado != null) {
                predicates.add(cb.equal(root.get("estado").get("idEstado"), idEstado));
            }
            if (nroFactura != null && !nroFactura.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nroFactura")), "%" + nroFactura.toLowerCase() + "%"));
            }
            if (codigoFactura != null && !codigoFactura.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("codigoFactura")), "%" + codigoFactura.toLowerCase() + "%"));
            }
            if (esCredito != null) {
                predicates.add(cb.equal(root.get("esCredito"), esCredito));
            }
            if (fechaEmisionDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaEmision"), fechaEmisionDesde));
            }
            if (fechaEmisionHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaEmision"), fechaEmisionHasta));
            }
            if (fechaVencimientoDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaVencimiento"), fechaVencimientoDesde));
            }
            if (fechaVencimientoHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaVencimiento"), fechaVencimientoHasta));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
