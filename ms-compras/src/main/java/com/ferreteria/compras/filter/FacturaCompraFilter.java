package com.ferreteria.compras.filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

import com.ferreteria.compras.service.SpecificationFilter;
import com.ferreteria.entity.compras.FacturaCompra;

/**
 * Criterios de busqueda de FacturaCompra. Los campos nulos se ignoran.
 * Las facturas inactivas (borrado logico) nunca se devuelven: lo garantiza el servicio.
 */
@Data
public class FacturaCompraFilter implements SpecificationFilter<FacturaCompra> {

    private Long idProveedor;
    private Long idEstado;
    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String nroFactura;
    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String codigoFactura;
    private BigDecimal timbrado;
    /** Formato ISO yyyy-MM-dd, inclusive. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEmisionDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEmisionHasta;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimientoDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimientoHasta;
    private BigDecimal importeMin;
    private BigDecimal importeMax;

    @Override
    public Specification<FacturaCompra> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idProveedor != null) {
                predicates.add(cb.equal(root.get("proveedor").get("idPersona"), idProveedor));
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
            if (timbrado != null) {
                predicates.add(cb.equal(root.<BigDecimal>get("timbrado"), timbrado));
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
            if (importeMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("importeTotal"), importeMin));
            }
            if (importeMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("importeTotal"), importeMax));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
