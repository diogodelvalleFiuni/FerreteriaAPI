# eureka-server

Registro y descubrimiento de servicios (Netflix Eureka). Spring Boot 3.5.15 / Spring Cloud 2025.0.3, Java 21.

## Ejecutar
    ./mvnw spring-boot:run          (Windows: mvnw.cmd spring-boot:run)

o ejecutar `EurekaServerApplication` desde el IDE. Panel: http://localhost:8761

## Como funciona
- Cada microservicio (y el api-gateway) se registra aca al encender: nombre, host y puerto.
- Despues envia un heartbeat cada 30 s (por defecto). Si deja de enviarlo ~90 s, Eureka lo saca de la lista.
- El api-gateway consulta esta lista para reenviar peticiones a rutas `lb://nombre-del-servicio`.
- Los clientes apuntan aca con `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`.

## Variables de entorno (opcionales)
| Variable | Por defecto | Para que sirve |
|---|---|---|
| `SERVER_PORT` / `SERVER_ADDRESS` | `8761` / `localhost` | Puerto y direccion (`0.0.0.0` para recibir registros de otras maquinas) |
| `EUREKA_SELF_PRESERVATION` | `false` | Autopreservacion (true en produccion) |
| `EUREKA_EVICTION_INTERVAL_MS` | `10000` | Cada cuanto elimina instancias vencidas |
