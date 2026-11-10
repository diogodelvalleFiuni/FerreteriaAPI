package com.ferreteria.ventas.controller;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.ferreteria.ventas.service.CrudService;

/**
 * Controller base con los endpoints REST comunes a todos los modulos.
 * Cada controller concreto solo agrega {@code @RestController} +
 * {@code @RequestMapping("/api/...")} + {@code @Tag} y el constructor, ej:
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
 *     <li>POST /api/{recurso}/search - busqueda paginada por Filter (body) + Pageable (query params)</li>
 * </ul>
 * Autorizacion por metodo ({@code @PreAuthorize} + {@code RoleAccess}, roles en {@code app.security.roles.*}):
 * lecturas (GET y search) -> rol de lectura; POST/PUT -> rol de escritura; DELETE -> rol de borrado.
 * La misma regla se aplica por URL en {@code SecurityConfig}.
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
    @PreAuthorize("@roleAccess.canRead(authentication)")
    @Operation(summary = "Listar registros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de registros")
    public Page<D> findAll(@ParameterObject Pageable pageable) {
        log.info("GET lista paginada: {}", pageable);
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@roleAccess.canRead(authentication)")
    @Operation(summary = "Obtener un registro por id")
    @ApiResponse(responseCode = "200", description = "Registro encontrado")
    @ApiResponse(responseCode = "404", description = "No existe el registro")
    public D findById(@PathVariable ID id) {
        log.info("GET por id: {}", id);
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("@roleAccess.canWrite(authentication)")
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
    @PreAuthorize("@roleAccess.canWrite(authentication)")
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
    @PreAuthorize("@roleAccess.canDelete(authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un registro")
    @ApiResponse(responseCode = "204", description = "Registro eliminado")
    @ApiResponse(responseCode = "404", description = "No existe el registro")
    public void delete(@PathVariable ID id) {
        log.info("DELETE id: {}", id);
        service.delete(id);
    }

    @PostMapping("/search")
    @PreAuthorize("@roleAccess.canRead(authentication)")
    @Operation(summary = "Buscar con filtros (paginado)")
    @ApiResponse(responseCode = "200", description = "Pagina de resultados")
    public Page<D> search(@RequestBody(required = false) F filter, @ParameterObject Pageable pageable) {
        log.info("POST search: {}", pageable);
        log.debug("POST search, filtro: {}", filter);
        return service.search(filter, pageable);
    }
}
