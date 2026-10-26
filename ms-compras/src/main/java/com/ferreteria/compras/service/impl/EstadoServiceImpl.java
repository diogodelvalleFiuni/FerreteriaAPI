package com.ferreteria.compras.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.compras.repository.EstadoRepository;
import com.ferreteria.compras.service.EstadoService;
import com.ferreteria.entity.general.Estado;

@Service
public class EstadoServiceImpl implements EstadoService {

    private static final Logger log = LoggerFactory.getLogger(EstadoServiceImpl.class);

    private final EstadoRepository repository;
    private final String nombreActivo;
    private final String nombreInactivo;

    public EstadoServiceImpl(EstadoRepository repository,
                             @Value("${app.compras.estado.activo}") String nombreActivo,
                             @Value("${app.compras.estado.inactivo}") String nombreInactivo) {
        this.repository = repository;
        this.nombreActivo = nombreActivo;
        this.nombreInactivo = nombreInactivo;
    }

    @Override
    @Transactional
    public Estado obtenerActivo() {
        return buscarOCrear(nombreActivo);
    }

    @Override
    @Transactional
    public Estado obtenerInactivo() {
        return buscarOCrear(nombreInactivo);
    }

    @Override
    public String nombreInactivo() {
        return nombreInactivo;
    }

    private Estado buscarOCrear(String nombre) {
        return repository.findFirstByNombreIgnoreCase(nombre).orElseGet(() -> {
            log.info("El estado '{}' no existe en la tabla estados; se crea", nombre);
            Estado estado = new Estado();
            estado.setNombre(nombre);
            return repository.save(estado);
        });
    }
}
