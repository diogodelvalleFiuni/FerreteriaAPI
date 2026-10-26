package com.ferreteria.inventario.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

import com.ferreteria.entity.Inventario.Transferencia;
import com.ferreteria.inventario.service.SpecificationFilter;

/** Criterios de busqueda de Transferencia (query params de GET /search). Los campos nulos se ignoran. */
@Data
public class TransferenciaFilter implements SpecificationFilter<Transferencia> {

    private Long idDepositoOrigen;
    private Long idDepositoDestino;
    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String creador;
    /**
     * Formato ISO yyyy-MM-dd, ambos extremos inclusive. La columna es fecha y hora, asi que
     * "desde" compara contra las 00:00 de ese dia y "hasta" cubre el dia completo.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaCreacionDesde;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaCreacionHasta;

    @Override
    public Specification<Transferencia> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (idDepositoOrigen != null) {
                predicates.add(cb.equal(root.get("depositoOrigen").get("idDeposito"), idDepositoOrigen));
            }
            if (idDepositoDestino != null) {
                predicates.add(cb.equal(root.get("depositoDestino").get("idDeposito"), idDepositoDestino));
            }
            if (creador != null && !creador.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("creador")), "%" + creador.toLowerCase() + "%"));
            }
            if (fechaCreacionDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fechaCreacion"), fechaCreacionDesde.atStartOfDay()));
            }
            if (fechaCreacionHasta != null) {
                predicates.add(cb.lessThan(root.<LocalDateTime>get("fechaCreacion"), fechaCreacionHasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
