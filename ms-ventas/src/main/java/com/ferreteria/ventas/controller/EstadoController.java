package com.ferreteria.ventas.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.ventas.repository.EstadoRepository;

/** Catalogo de estados (solo lectura). Devuelve una proyeccion, no la entidad. */
@RestController
@RequestMapping("/api/estados")
@Tag(name = "Estados", description = "Catalogo de estados (solo lectura)")
public class EstadoController {

    private final EstadoRepository repository;

    public EstadoController(EstadoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<EstadoResumen> listar() {
        return repository.findAll().stream()
                .map(e -> new EstadoResumen(e.getIdEstado(), e.getNombre()))
                .toList();
    }

    public record EstadoResumen(Long idEstado, String nombre) {
    }
}
