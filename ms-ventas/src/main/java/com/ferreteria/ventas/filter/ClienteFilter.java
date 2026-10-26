package com.ferreteria.ventas.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

import com.ferreteria.entity.ventas.Cliente;
import com.ferreteria.ventas.service.SpecificationFilter;

/** Criterios de busqueda de Cliente (incluye datos de Persona). Los campos nulos se ignoran. */
@Data
public class ClienteFilter implements SpecificationFilter<Cliente> {

    /** Coincidencia parcial, sin distinguir mayusculas. */
    private String nombre;
    private String apellido;
    private String ruc;
    private String correo;
    /** Coincidencia parcial. */
    private String ci;
    /** Si es true, solo clientes con saldo pendiente (saldoActual mayor a 0). */
    private Boolean conSaldoPendiente;

    @Override
    public Specification<Cliente> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null && !nombre.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (apellido != null && !apellido.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellido")), "%" + apellido.toLowerCase() + "%"));
            }
            if (ruc != null && !ruc.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("ruc")), "%" + ruc.toLowerCase() + "%"));
            }
            if (correo != null && !correo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("correo")), "%" + correo.toLowerCase() + "%"));
            }
            if (ci != null && !ci.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("ci")), "%" + ci.toLowerCase() + "%"));
            }
            if (Boolean.TRUE.equals(conSaldoPendiente)) {
                predicates.add(cb.greaterThan(root.<BigDecimal>get("saldoActual"), BigDecimal.ZERO));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
