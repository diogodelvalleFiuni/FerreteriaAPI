package com.ferreteria.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ferreteria.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    /** Incluye usuarios deshabilitados (el username es unico en toda la tabla, no solo entre los activos). */
    boolean existsByUsername(String username);
}
