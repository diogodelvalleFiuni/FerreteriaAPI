package com.ferreteria.seguridad.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.ferreteria.seguridad.service.UsuarioService;

/**
 * Crea el primer ADMIN al arrancar, solo si se definieron BOOTSTRAP_ADMIN_USER / BOOTSTRAP_ADMIN_PASSWORD
 * y no existe ningun usuario. No hay usuario ni clave por defecto: sin las variables no se crea nada.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final BootstrapProperties props;
    private final UsuarioService usuarioService;

    public AdminBootstrap(BootstrapProperties props, UsuarioService usuarioService) {
        this.props = props;
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.configured()) {
            return;
        }
        if (usuarioService.crearAdminInicial(props.adminUsername(), props.adminPassword())) {
            log.warn("Se creo el administrador inicial '{}'. Quite BOOTSTRAP_ADMIN_USER y BOOTSTRAP_ADMIN_PASSWORD "
                    + "de la configuracion.", props.adminUsername().trim());
        } else {
            log.info("Bootstrap omitido: ya existen usuarios. Puede quitar BOOTSTRAP_ADMIN_USER y BOOTSTRAP_ADMIN_PASSWORD.");
        }
    }
}
