package com.ferreteria.ventas.service;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.ventas.filter.ClienteFilter;

/** Operaciones de negocio de Cliente. La implementacion esta en service/impl. */
public interface ClienteService extends CrudService<ClienteDTO, ClienteFilter, Long> {
}
