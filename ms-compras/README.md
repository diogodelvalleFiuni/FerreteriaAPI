# ms-compras

Microservicio de **compras** de la Ferreteria (Spring Boot 4.1 / Java 21). Estructura identica a `ms-inventario`:
`controller -> service -> repository` (sin saltos de capa), MapStruct para DTO <-> Entity, Spring Data JPA,
Swagger/OpenAPI, logs a disco con Logback y registro en Eureka. Entidades y DTO vienen del JAR `libs/commons` (sin modificar).

## Recursos

| Recurso | Entidad (commons) | Notas |
|---|---|---|
| `/api/proveedores` | `Proveedor` (extiende `Persona`) | Entidad simple. RUC unico. DELETE borra la fila (409 si tiene facturas). |
| `/api/facturas-compras` | `FacturaCompra` | **DELETE = borrado logico** (estado "inactivo"). `importeTotal` lo calcula el servidor. |
| `/api/detalles-compras` | `DetalleFacturaCompra` | Entidad asociativa del **muchos-a-muchos Factura - Producto**. IVA y subtotal calculados por el servidor. |

Cada recurso expone:

| Operacion | Endpoint | Respuesta |
|---|---|---|
| Crear | `POST /api/{recurso}` | 201 |
| Actualizar | `PUT /api/{recurso}/{id}` | 200 / 404 |
| Obtener por id | `GET /api/{recurso}/{id}` | 200 / 404 |
| Listar (paginado) | `GET /api/{recurso}?page=0&size=20&sort=id,asc` | 200 |
| Buscar con filtros (paginado) | `GET /api/{recurso}/search?page=0&size=20&<filtros>` (ej. `?nroFactura=001&fechaEmisionDesde=2026-01-01`) | 200 |
| Borrar | `DELETE /api/{recurso}/{id}` | 204 / 404 |

Errores: siempre `ApiErrorDTO` (400 validacion o regla de negocio, 404, 409 duplicado/FK, 500 sin detalles tecnicos).

Swagger UI: `http://localhost:8083/swagger-ui.html`.

## Reglas de negocio

* **Factura**: vencimiento >= emision; no se repite proveedor + nro de factura + timbrado (409). Al crearla queda en estado
  "activo" e `importeTotal = 0`.
* **Borrado logico**: `DELETE` asigna a la factura el estado cuyo nombre es `app.compras.estado.inactivo` (se crea en la tabla
  `estados` si no existe). Las facturas inactivas, y sus detalles, no se listan y devuelven 404 por id.
* **Detalle**: la factura debe existir y estar activa; cantidad > 0; IVA en `app.compras.iva.porcentajes-validos`
  (por defecto 0, 5, 10; si no viene, 10). `montoIva` y `subtotal` se calculan en el servidor
  (`app.compras.iva.incluido-en-precio=true` -> IVA = subtotal x p / (100 + p)). Al crear, modificar o borrar un detalle se
  recalcula `importeTotal` de la factura. Un detalle no se puede mover a otra factura.
* `proveedores` y `detalles_facturas_compras` no tienen columna de estado en commons, por eso su DELETE borra la fila.

## Ejecutar

1. Copiar `local.properties.example` a `local.properties` y completar `DB_PSWD`, `JWT_SECRET` (la MISMA que ms-seguridad e
   ms-inventario) y los datos de Redis (la MISMA instancia que usan los demas servicios).
2. Levantar Eureka (`eureka-server`, puerto 8761) y luego `./mvnw spring-boot:run` o `mvn spring-boot:run`. Escucha en `localhost:8083`.
3. Pedir un token en ms-seguridad (`POST /api/auth/login`) y llamar a Compras con `Authorization: Bearer <token>`
   (en Swagger: boton **Authorize**).

El esquema de la base lo administra **commons** (Liquibase); aqui `spring.jpa.hibernate.ddl-auto=none`.

## Configuracion (todo por variable de entorno, con valor por defecto)

Mismas variables que ms-inventario (`SERVER_PORT` = 8083, `DB_*`, `EUREKA_*`, `LOG_*`, `SWAGGER_*`, `PAGE_*`) mas:

| Variable | Por defecto | Para que sirve |
|---|---|---|
| `COMPRAS_ESTADO_ACTIVO` | `Activo` | Nombre del estado de una factura nueva |
| `COMPRAS_ESTADO_INACTIVO` | `Inactivo` | Nombre del estado que marca el borrado logico |
| `COMPRAS_IVA_VALIDOS` | `0,5,10` | Porcentajes de IVA aceptados |
| `COMPRAS_IVA_DEFECTO` | `10` | IVA si el detalle no lo informa |
| `COMPRAS_IVA_INCLUIDO` | `true` | El precio unitario ya incluye IVA |
| `COMPRAS_MONTO_ESCALA` | `2` | Decimales de montos |

Logs en `./logs/ms-compras.log` y `./logs/ms-compras-error.log` (INFO en controller/service, SQL de Hibernate en DEBUG).

## Seguridad (JWT + roles)

Mismo contrato que ms-inventario: Compras **valida localmente** el access token emitido por ms-seguridad (firma HS256,
expiracion y, si se define, emisor), sin llamar a ningun servicio. Un refresh token se rechaza porque va firmado con otra clave.

| Nivel | Donde | Regla |
|---|---|---|
| URL | `security/SecurityConfig` | Swagger publico; `DELETE /api/**` solo `ADMIN`; el resto exige token valido |
| Metodo | `@PreAuthorize("hasRole('ADMIN')")` en `AbstractBaseController.delete` | segunda barrera para el borrado |

* Sin token o token invalido -> **401**; token valido sin el rol -> **403**. Siempre `ApiErrorDTO`, sin trazas.
* Los roles del claim `roles` se mapean a `ROLE_xxx` (si ya traen el prefijo, no se duplica).
* Acceder directo a `localhost:8083` tampoco evita la validacion: el filtro corre en este servicio.

## Cache (Redis)

* Una seccion por dominio, clave = id, prefijo propio: `compras::proveedores::<id>`, `compras::facturas-compra::<id>`,
  `compras::detalles-factura-compra::<id>` (no se mezclan con las de Inventario).
* `findById` -> `@Cacheable`; `create` -> `@CachePut`; `update`/`delete` -> `@CacheEvict`.
* **Busquedas y listados no se cachean como pagina**: `GET /search` guarda cada DTO de la pagina por separado.
* **Invalidaciones cruzadas**: al crear, modificar o borrar un detalle se recalcula el importe y se invalida la factura
  (`actualizarImporteTotal`); al modificar o borrar una factura se vacia la seccion de detalles (los detalles de una factura
  inactiva dejan de ser visibles).
* `put`/`evict` se aplican despues del commit (`transactionAware`) para no cachear datos de una transaccion que se revierte.
* No se guardan nulos. Valores en JSON tipado por DTO (sin default typing).
* **Trazabilidad**: `LoggingCache` registra cada GET (HIT/MISS), SET, DELETE y CLEAR con seccion, clave y duracion
  (nunca el valor). Ejemplo: `CACHE GET seccion=proveedores clave=1 resultado=HIT (180 us)`.
* **Redis es opcional** (`ResilientCacheManager`): si Redis cae, no responde o devuelve un dato ilegible, la operacion se
  trata como "no esta en la cache" y la peticion sigue contra PostgreSQL, sin error. El primer fallo abre un corte de
  `CACHE_RETRY_SECONDS` (30 s por defecto) durante el cual no se vuelve a consultar Redis (lecturas = miss, escrituras e
  invalidaciones = no-op); pasado ese tiempo se reintenta y la cache retoma sola. Se registra un solo WARN por corte.
  Las invalidaciones omitidas durante un corte dejan la entrada vieja en Redis hasta que venza su TTL.
  El pool Hikari (`DB_POOL_MAX`) acota la carga sobre PostgreSQL mientras Redis esta caido.

| Variable | Por defecto | Para que sirve |
|---|---|---|
| `COMPRAS_ESTADO_ACTIVO` | `Activo` | Nombre del estado de una factura nueva |
| `COMPRAS_ESTADO_INACTIVO` | `Inactivo` | Nombre del estado que marca el borrado logico |
| `COMPRAS_IVA_VALIDOS` | `0,5,10` | Porcentajes de IVA aceptados |
| `COMPRAS_IVA_DEFECTO` | `10` | IVA si el detalle no lo informa |
| `COMPRAS_IVA_INCLUIDO` | `true` | El precio unitario ya incluye IVA |
| `COMPRAS_MONTO_ESCALA` | `2` | Decimales de montos |

Logs en `./logs/ms-compras.log` y `./logs/ms-compras-error.log` (INFO en controller/service, SQL de Hibernate en DEBUG).

## Seguridad (JWT + roles)

Mismo contrato que ms-inventario: Compras **valida localmente** el access token emitido por ms-seguridad (firma HS256,
expiracion y, si se define, emisor), sin llamar a ningun servicio. Un refresh token se rechaza porque va firmado con otra clave.

| Nivel | Donde | Regla |
|---|---|---|
| URL | `security/SecurityConfig` | Swagger publico; `DELETE /api/**` solo `ADMIN`; el resto exige token valido |
| Metodo | `@PreAuthorize("hasRole('ADMIN')")` en `AbstractBaseController.delete` | segunda barrera para el borrado |

* Sin token o token invalido -> **401**; token valido sin el rol -> **403**. Siempre `ApiErrorDTO`, sin trazas.
* Los roles del claim `roles` se mapean a `ROLE_xxx` (si ya traen el prefijo, no se duplica).
* Acceder directo a `localhost:8083` tampoco evita la validacion: el filtro corre en este servicio.

## Cache (Redis)

* Una seccion por dominio, clave = id, prefijo propio: `compras::proveedores::<id>`, `compras::facturas-compra::<id>`,
  `compras::detalles-factura-compra::<id>` (no se mezclan con las de Inventario).
* `findById` -> `@Cacheable`; `create` -> `@CachePut`; `update`/`delete` -> `@CacheEvict`.
* **Busquedas y listados no se cachean como pagina**: `GET /search` guarda cada DTO de la pagina por separado.
* **Invalidaciones cruzadas**: al crear, modificar o borrar un detalle se recalcula el importe y se invalida la factura
  (`actualizarImporteTotal`); al modificar o borrar una factura se vacia la seccion de detalles (los detalles de una factura
  inactiva dejan de ser visibles).
* `put`/`evict` se aplican despues del commit (`transactionAware`) para no cachear datos de una transaccion que se revierte.
* No se guardan nulos. Valores en JSON tipado por DTO (sin default typing).
* **Trazabilidad**: `LoggingCache` registra cada GET (HIT/MISS), SET, DELETE y CLEAR con seccion, clave y duracion
  (nunca el valor). Ejemplo: `CACHE GET seccion=proveedores clave=1 resultado=HIT (180 us)`.
* **Si Redis cae** (`CACHE_FAIL_OPEN=true`): se registra `CACHE ERROR ...` y la peticion sigue contra PostgreSQL; el pool Hikari
  (`DB_POOL_MAX`) y el timeout de Redis (`REDIS_TIMEOUT_MS`) acotan la carga. Con `false` el error se propaga.

| Variable | Por defecto | Para que sirve |
|---|---|---|
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PSWD` / `REDIS_DB` | `localhost` / `6379` / vacio / `0` | Redis compartido |
| `REDIS_TIMEOUT_MS` / `REDIS_CONNECT_TIMEOUT_MS` | `1000ms` | Cuanto espera la app por Redis |
| `CACHE_TTL_MINUTES` | `5` | Vida de cada entrada (todas tienen TTL; ninguna es persistente) |
| `CACHE_KEY_PREFIX` | `compras::` | Prefijo de claves |
| `CACHE_RETRY_SECONDS` | `30` | Tras un fallo, segundos sin consultar Redis antes de reintentar |
| `CACHE_LOG_LEVEL` | `INFO` | Nivel con el que se registran las operaciones de cache |
| `JWT_SECRET` / `JWT_SECRET_BASE64` / `JWT_ALGORITHM` / `JWT_ISSUER` / `JWT_ROLES_CLAIM` | ver `application.properties` | Mismos valores que ms-seguridad |

### Prueba rapida de cache e invalidacion
1. Login en ms-seguridad, copiar el `accessToken`, **Authorize** en Swagger de Compras.
2. `GET /api/facturas-compras/{id}` dos veces: en el log, `MISS` + `SET` la primera y `HIT` la segunda.
3. `POST /api/detalles-compras` sobre esa factura y volver a hacer `GET` de la factura: `DELETE` de la clave en el log, `MISS`
   y el `importeTotal` ya actualizado.
4. Sin token -> 401; con un usuario sin rol `ADMIN`, `DELETE` -> 403.
