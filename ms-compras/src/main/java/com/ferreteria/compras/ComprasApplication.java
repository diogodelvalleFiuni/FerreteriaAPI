package com.ferreteria.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Las entidades JPA viven en el JAR commons (paquete com.ferreteria.entity), fuera del
 * paquete de esta aplicacion, por eso hay que indicarlo con {@code @EntityScan}.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.ferreteria.entity")
@EnableCaching
public class ComprasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComprasApplication.class, args);
    }
}
