package com.ferreteria.seguridad.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ferreteria.commons.dto.UsuarioDTO;
import com.ferreteria.seguridad.service.UsuarioService;

/**
 * Alta de usuarios, solo ADMIN (la regla esta en SecurityConfig y se repite aca con @PreAuthorize).
 * Body: {@code {"username":"juan","password":"...","roles":[{"nombre":"VENDEDOR"}]}}.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioDTO crear(@Valid @RequestBody UsuarioDTO request) {
        return usuarioService.crear(request);
    }
}
