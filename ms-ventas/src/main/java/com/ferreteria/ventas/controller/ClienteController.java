package com.ferreteria.ventas.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ferreteria.commons.dto.ClienteDTO;
import com.ferreteria.ventas.filter.ClienteFilter;
import com.ferreteria.ventas.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Clientes de la ferreteria (datos de persona + limite y saldo)")
public class ClienteController
        extends AbstractBaseController<ClienteDTO, ClienteFilter, Long, ClienteService> {

    public ClienteController(ClienteService service) {
        super(service);
    }
}
