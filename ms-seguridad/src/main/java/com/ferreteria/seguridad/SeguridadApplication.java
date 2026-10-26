package com.ferreteria.seguridad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Las entidades JPA viven en el JAR commons (paquete com.ferreteria.entity), fuera del
 * paquete de esta aplicacion, por eso hay que indicarlo con {@code @EntityScan}.
 * El registro en Eureka es automatico (eureka-client en el classpath).
 */
@SpringBootApplication
@EntityScan(basePackages = "com.ferreteria.entity")
public class SeguridadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeguridadApplication.class, args);
    }
}
