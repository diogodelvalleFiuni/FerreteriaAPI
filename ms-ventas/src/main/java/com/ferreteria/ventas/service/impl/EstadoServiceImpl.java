package com.ferreteria.ventas.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferreteria.entity.general.Estado;
import com.ferreteria.ventas.repository.EstadoRepository;
import com.ferreteria.ventas.service.EstadoService;

/**
 * Configuracion (application.properties):
 * <ul>
 *     <li>{@code app.ventas.estado.inactivo-id} - id fijo del estado inactivo (opcional, tiene prioridad)</li>
 *     <li>{@code app.ventas.estado.inactivo-nombre} - nombre a buscar en la tabla estados si no hay id</li>
 * </ul>
 * El id se resuelve una sola vez y queda en cache.
 */
@Service
public class EstadoServiceImpl implements EstadoService {

    private static final Logger log = LoggerFactory.getLogger(EstadoServiceImpl.class);

    private final EstadoRepository repository;
    private final String inactivoIdConfigurado;
    private final String inactivoNombre;

    private volatile Long inactivoId;

    public EstadoServiceImpl(EstadoRepository repository,
                             @Value("${app.ventas.estado.inactivo-id:}") String inactivoIdConfigurado,
                             @Value("${app.ventas.estado.inactivo-nombre:Inactivo}") String inactivoNombre) {
        this.repository = repository;
        this.inactivoIdConfigurado = inactivoIdConfigurado;
        this.inactivoNombre = inactivoNombre;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getInactivoId() {
        Long cached = inactivoId;
        if (cached != null) {
            return cached;
        }
        Long resolved = resolver();
        inactivoId = resolved;
        log.info("Estado inactivo para borrado logico: id {}", resolved);
        return resolved;
    }

    @Override
    public Estado getInactivo() {
        Estado estado = new Estado();
        estado.setIdEstado(getInactivoId());
        return estado;
    }

    private Long resolver() {
        if (inactivoIdConfigurado != null && !inactivoIdConfigurado.isBlank()) {
            return Long.valueOf(inactivoIdConfigurado.trim());
        }
        log.debug("Buscando el estado inactivo por nombre '{}'", inactivoNombre);
        return repository.findFirstByNombreIgnoreCase(inactivoNombre)
                .map(Estado::getIdEstado)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado '" + inactivoNombre + "' en la tabla estados; "
                                + "configure app.ventas.estado.inactivo-id o app.ventas.estado.inactivo-nombre"));
    }
}
