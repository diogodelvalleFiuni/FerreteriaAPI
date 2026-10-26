package com.ferreteria.compras.service.support;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import com.ferreteria.entity.general.Estado;

/**
 * Regla unica del borrado logico: un registro esta inactivo si su {@link Estado} se llama como el estado
 * configurado en {@code app.compras.estado.inactivo} (sin distinguir mayusculas). Sin estado = activo.
 */
public final class EstadoVisibilidad {

    private EstadoVisibilidad() {
    }

    /** Predicado para consultas: el elemento {@code from} (que tiene un atributo "estado") esta activo. */
    public static Predicate estadoActivo(From<?, ?> from, CriteriaBuilder cb, String nombreInactivo) {
        Join<Object, Object> estado = from.join("estado", JoinType.LEFT);
        return cb.or(
                cb.isNull(estado.get("idEstado")),
                cb.notEqual(cb.lower(estado.<String>get("nombre")), nombreInactivo.toLowerCase()));
    }

    /** Version en memoria del mismo criterio, para una entidad ya cargada. */
    public static boolean esInactivo(Estado estado, String nombreInactivo) {
        return estado != null && estado.getNombre() != null && estado.getNombre().equalsIgnoreCase(nombreInactivo);
    }
}
