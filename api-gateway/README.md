# api-gateway

Spring Cloud Gateway (MVC) + cliente Eureka + validacion JWT. Spring Boot 3.5.15 / Spring Cloud 2025.0.3, Java 21.

## Ejecutar
Requiere el Eureka Server en `http://localhost:8761/eureka/` (si no esta, el gateway igual arranca y reintenta;
con `EUREKA_ENABLED=false` ni lo intenta).

    ./mvnw spring-boot:run          (Windows: mvnw.cmd spring-boot:run)

o ejecutar `ApiGatewayApplication` desde el IDE. Escucha en `http://localhost:8080`.

## Variables de entorno (todas opcionales)
| Variable | Por defecto | Para que sirve |
|---|---|---|
| `SERVER_PORT` / `SERVER_ADDRESS` | `8080` / `localhost` | Puerto y direccion |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Servidor Eureka |
| `EUREKA_ENABLED` | `true` | `false` = sin Eureka |
| `INVENTARIO_URI` | `lb://ms-inventario` | Destino de ms-inventario (via Eureka). `http://localhost:8081` = directo, sin Eureka |
| `VENTAS_URI` | `lb://ms-ventas` | Destino de ms-ventas (`http://localhost:8082` = directo) |
| `CONTEXT_PATH` | `/` | Contexto (prefijo) del gateway |
| `LOG_FILE` | `logs/api-gateway.log` | Archivo de log |
| `JWT_ENABLED` | `true` | `false` = no valida tokens (solo pruebas) |
| `JWT_SECRET` | `cambia-esto-...` | Debe ser el mismo secret del servicio de autenticacion |
| `JWT_PUBLIC_PATHS` | `/auth/,/docs/,/webjars/,/swagger-ui,/v3/api-docs,/actuator/health` | Rutas sin token |

## Errores y logs
- Todo error del gateway (token invalido, servicio sin instancias en Eureka `503`, servicio caido `502`, timeout `504`,
  ruta inexistente `404`, error inesperado `500`) se devuelve con el mismo formato que ms-inventario:
  `timestamp`, `status`, `error`, `message`, `path`. El detalle tecnico solo queda en el log.
- Logs a disco: `logs/api-gateway.log` (`LOG_FILE` para cambiarlo).
- `CONTEXT_PATH` define un prefijo para todo el gateway (por defecto `/`).

## Swagger comun
- **http://localhost:8080/swagger-ui.html**: una sola interfaz con un selector de microservicio (Inventario, Ventas).
  Cada servicio sigue documentandose solo (`/v3/api-docs`); el gateway reenvia `/docs/<servicio>` a ese endpoint
  (rutas `*-docs`) y la interfaz lista los servicios de `app.swagger.definitions`.
- "Try it out" pasa por el gateway (`PreserveHostHeader`). Con `JWT_ENABLED=true` las pruebas requieren token.
- Swagger propio de cada servicio, sin gateway: http://localhost:8081/swagger-ui.html (inventario),
  http://localhost:8082/swagger-ui.html (ventas).

## Rutas
| Prefijo | Servicio (Eureka) | Destino directo |
|---|---|---|
| `/api/productos`, `/api/marcas`, `/api/categorias-productos`, `/api/depositos`, `/api/inventarios`, `/api/transferencias`, `/api/detalles-transferencia` | `ms-inventario` | `INVENTARIO_URI` (`http://localhost:8081`) |
| `/api/clientes`, `/api/presupuestos`, `/api/presupuestos-detalle`, `/api/facturas-ventas`, `/api/detalles-facturas-ventas` | `ms-ventas` | `VENTAS_URI` (`http://localhost:8082`) |

## Agregar un microservicio nuevo (solo `application.yml`, sin codigo)
1. Ruta de la API: `Path=/api/<recurso>/**` hacia `lb://<nombre-en-eureka>`.
2. Ruta de su documentacion: `Path=/docs/<servicio>` con `SetPath=/v3/api-docs` y `PreserveHostHeader`.
3. Una entrada en `app.swagger.definitions` (`name` y `url: docs/<servicio>`).
