package com.ferreteria.compras.service;

import org.springframework.data.jpa.domain.Specification;

/**
 * Contrato que debe implementar todo objeto Filter usado por
 * {@link AbstractBaseService} para hacer busquedas dinamicas.
 * <p>
 * Cada Filter concreto arma su propio {@link Specification} combinando
 * los criterios que traiga seteados (los nulos se ignoran), ej:
 * <pre>{@code
 * public class ProductoFilter implements SpecificationFilter<Producto> {
 *     private String nombre;
 *     private Long categoriaId;
 *
 *     public Specification<Producto> toSpecification() {
 *         return (root, query, cb) -> {
 *             var predicates = new ArrayList<Predicate>();
 *             if (nombre != null) {
 *                 predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
 *             }
 *             if (categoriaId != null) {
 *                 predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
 *             }
 *             return cb.and(predicates.toArray(new Predicate[0]));
 *         };
 *     }
 * }
 * }</pre>
 *
 * @param <E> tipo de entidad sobre la que se filtra
 */
public interface SpecificationFilter<E> {

    Specification<E> toSpecification();
}
