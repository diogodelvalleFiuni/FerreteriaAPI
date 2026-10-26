package com.ferreteria.seguridad.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ferreteria.entity.general.Estado;

/** Estado viene de commons (tabla estados); aca solo se lee para asignar el estado "activo" a los usuarios nuevos. */
public interface EstadoRepository extends JpaRepository<Estado, Long> {
}
