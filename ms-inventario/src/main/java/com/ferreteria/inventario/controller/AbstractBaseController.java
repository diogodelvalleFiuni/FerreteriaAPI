package com.ferreteria.inventario.controller;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

import com.ferreteria.inventario.service.CrudService;

/**
 * Controller base con los endpoints REST comunes a todos los modulos.
 * Cada controller concreto agrega {@code @RestController}, {@code @RequestMapping("/api/...")},
 * {@code @Tag}, el constructor y el metodo {@code GET /search} (que delega en {@link #searchInternal}), ej:
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/depositos")
 * public class DepositoController
 *         extends AbstractBaseController<DepositoDTO, DepositoFilter, Long, DepositoService> {
 *     public DepositoController(DepositoService service) {
 *         super(service);
 *     }
 * }
 * }</pre>
 * Endpoints expuestos (el controller solo delega en el service):
 * <ul>
 *     <li>GET /api/{recurso} - lista paginada (?page=&amp;size=&amp;sort=)</li>
 *     <li>GET /api/{recurso}/{id} - busca por id (404 si no existe)</li>
 *     <li>POST /api/{recurso} - crea (201)</li>
 *     <li>PUT /api/{recurso}/{id} - actualiza</li>
 *     <li>DELETE /api/{recurso}/{id} - elimina (204)</li>
 *     <li>GET /api/{recurso}/search - busqueda paginada: campos del Filter + Pageable como query params</li>
 * </ul>
 *
 * @param <D>  DTO expuesto
 * @param <F>  Filter de busqueda
 * @param <ID> tipo del identificador
 * @param <S>  Service concreto, extiende {@link CrudService}
 */
public abstract class AbstractBaseController<D, F, ID extends Serializable, S extends CrudService<D, F, ID>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final S service;

    protected AbstractBaseController(S service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar registros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de registros")
    public Page<D> findAll(@ParameterObject Pageable pageable) {
        log.info("GET lista paginada: {}", pageable);
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un registro por id")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    @ApiResponse(responseCode = "404", description = "No existe el registro")
    public D findById(@PathVariable ID id) {
        log.info("GET por id: {}", id);
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un registro")
    @ApiResponse(responseCode = "201", description = "Registro creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    public D create(@Valid @RequestBody D dto) {
        log.info("POST crear");
        log.debug("POST crear, cuerpo: {}", dto);
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un registro")
    @ApiResponse(responseCode = "200", description = "Registro actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "No existe el registro")
    public D update(@PathVariable ID id, @Valid @RequestBody D dto) {
        log.info("PUT actualizar id: {}", id);
        log.debug("PUT actualizar id {}, cuerpo: {}", id, dto);
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un registro")
    @ApiResponse(responseCode = "204", description = "Registro eliminado")
    @ApiResponse(responseCode = "404", description = "No existe el registro")
    public void delete(@PathVariable ID id) {
        log.info("DELETE id: {}", id);
        service.delete(id);
    }

    /**
     * Logica comun de la busqueda. El mapeo {@code GET /search} lo declara cada controller concreto
     * con su Filter concreto: asi Spring y Swagger enlazan los parametros del filtro sin depender
     * de resolver el tipo generico {@code F}.
     */
    protected Page<D> searchInternal(F filter, Pageable pageable) {
        log.info("GET search: {}", pageable);
        log.debug("GET search, filtro: {}", filter);
        return service.search(filter, pageable);
    }
}
