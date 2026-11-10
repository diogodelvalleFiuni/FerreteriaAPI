# ms-ventas

Microservicio de ventas (Ferreteria Lujan). Spring Boot 4.1 / Java 21, misma arquitectura que `ms-inventario`.
Usa entidades y DTOs del JAR `libs/commons-0.0.1-SNAPSHOT.jar` sin modificarlos.

## Ejecutar
Requisitos levantados: **Redis** (compartido), **ms-seguridad** (emite los tokens) y, para el Swagger unico y el ruteo,
**Eureka** + **api-gateway**.

1. Copiar `local.properties.example` a `local.properties` (o reutilizar el de ms-inventario) con `DB_*`, `JWT_SECRET`
   y, si hace falta, `REDIS_*`. **`JWT_SECRET` debe ser la misma clave que usa ms-seguridad.**
2. `mvn spring-boot:run` - puerto **8082** (configurable con `SERVER_PORT`).
3. Swagger directo: http://localhost:8082/swagger-ui.html (boton **Authorize** para pegar el token).
   Swagger unico: http://localhost:8080/swagger-ui.html (api-gateway, entrada "Ventas").

Para probar: `POST /api/auth/login` en ms-seguridad -> copiar el `accessToken` -> enviarlo como
`Authorization: Bearer <token>`.

## Recursos (todos con el mismo CRUD)
| Ruta | DTO | Entidad | DELETE |
|---|---|---|---|
| `/api/clientes` | ClienteDTO | Cliente (extiende Persona) | fisico (sin columna de estado) |
| `/api/presupuestos` | PresupuestoDTO | Presupuesto | **logico** (estado inactivo) |
| `/api/presupuestos-detalle` | PresupuestoDetalleDTO | PresupuestoDetalle (N:M Presupuesto-Producto) | fisico |
| `/api/facturas-ventas` | FacturaVentaDTO | FacturaVenta | **logico** (estado inactivo) |
| `/api/detalles-facturas-ventas` | DetalleFacturaVentaDTO | DetalleFacturaVenta (N:M FacturaVenta-Producto) | fisico |

Cada recurso: `GET /` (paginado), `GET /{id}`, `POST /` (201), `PUT /{id}`, `DELETE /{id}` (204),
`POST /search` (filtro en el body + `?page=&size=&sort=`).

## Seguridad (JWT + Spring Security)
ms-ventas **no emite ni administra usuarios**: eso es de ms-seguridad. Aca solo se valida el token y se autoriza.

- `security/AuthFilter`: toma `Authorization: Bearer ...`, comprueba el formato (3 secciones) y lo valida con un
  `JwtDecoder` de Spring (firma HS256, expiracion, emisor opcional). Publica un `UsuarioPrincipal` (usuario + roles) en
  el `SecurityContext`. Un refresh token no sirve: va firmado con otra clave.
- Sin token / token invalido o vencido -> **401**. Token valido sin el rol necesario -> **403**. Ambos con el mismo
  `ApiErrorDTO` de commons, sin trazas ni detalles internos (el motivo real solo queda en el log).
- Autorizacion en **dos capas**, ambas leyendo los mismos roles de `app.security.roles.*`:
  - por **URL**: `security/SecurityConfig` (`authorizeHttpRequests`)
  - por **metodo**: `@PreAuthorize("@roleAccess.canRead(authentication)")` y equivalentes en `AbstractBaseController`
- Un usuario puede tener **varios roles**: alcanza con uno de los listados para cada operacion.

| Operacion | Propiedad | Roles por defecto |
|---|---|---|
| `GET`, `POST .../search` | `app.security.roles.read` (`ROLES_READ`) | ADMIN, VENDEDOR |
| `POST`, `PUT` | `app.security.roles.write` (`ROLES_WRITE`) | ADMIN, VENDEDOR |
| `DELETE` | `app.security.roles.delete` (`ROLES_DELETE`) | ADMIN |

Los roles tienen que existir (activos) en la tabla `roles` y estar asignados al usuario en ms-seguridad
(`POST /api/usuarios`, solo ADMIN). Solo Swagger (`/swagger-ui`, `/v3/api-docs`) es publico.

La capa de **API Gateway** (validacion del token antes de rutear) ya la resuelve `api-gateway`; ms-ventas vuelve a
validar por su cuenta, asi que no depende de que la llamada haya pasado por el gateway.

## Cache (Redis compartido)
Mismo servidor Redis que ms-inventario, ms-seguridad y ms-cache-redis (`REDIS_HOST`, `REDIS_PORT`, ...).

- **Gestion**: abstraccion de Spring (`@EnableCaching`) + `CacheManager` creado por codigo en `config/CacheConfig`.
  `findById` -> `@Cacheable`; `create` -> `@CachePut`; `update` y `delete` -> `@CacheEvict`
  (en borrado logico tambien se invalida).
- **Segmentacion por dominio**: una seccion por servicio (`config/CacheNames`). Clave en Redis:
  `<prefijo><seccion>::<id>`, ej. `ventas::clientes::15`. Cada seccion se puede expirar o invalidar sola.
- **Busquedas y paginas**: la pagina completa **no** se cachea; cada DTO que contiene se guarda por separado con su
  id (`AbstractBaseService.cacheResults`). Si Redis falla en ese paso, el listado igual responde.
- **Retencion** (`app.cache.ttl-minutes.<seccion>`, `0` = persistente):

| Seccion | TTL por defecto | Variable |
|---|---|---|
| `clientes` | 30 min | `CACHE_TTL_CLIENTES_MIN` |
| `presupuestos` | 10 min | `CACHE_TTL_PRESUPUESTOS_MIN` |
| `presupuestos-detalle` | 10 min | `CACHE_TTL_PRESUPUESTOS_DETALLE_MIN` |
| `facturas-ventas` | **persistente** | `CACHE_TTL_FACTURAS_MIN` |
| `detalles-facturas-ventas` | **persistente** | `CACHE_TTL_DETALLES_FACTURAS_MIN` |

  Lo persistente igual se invalida al actualizar o borrar el objeto. `CACHE_KEY_PREFIX` cambia el prefijo.
- **Trazabilidad**: `cache/LoggingCache` registra cada operacion (GET con HIT/MISS, PUT, EVICT, CLEAR) con seccion y
  clave, en el logger `com.ferreteria.ventas.cache` (nivel `LOG_LEVEL_CACHE`, INFO por defecto). Nunca se loguea el valor.
- **Redis es opcional** (`config/ResilientCacheManager`): si Redis cae, no responde o devuelve un dato ilegible, la
  operacion se trata como "no esta en la cache" y la peticion sigue contra PostgreSQL, sin error. El primer fallo abre un
  corte de `CACHE_RETRY_SECONDS` (30 s por defecto) sin consultar Redis (lecturas = miss, escrituras e invalidaciones =
  no-op); luego se reintenta y la cache retoma sola. Timeouts cortos: `REDIS_TIMEOUT_MS` / `REDIS_CONNECT_TIMEOUT_MS`
  (1000 ms). **Ojo**: una invalidacion omitida durante un corte deja la entrada vieja hasta que venza su TTL; en las
  secciones persistentes (`facturas-ventas`, `detalles-facturas-ventas`) no vence, asi que conviene vaciar esas claves
  en Redis si hubo una caida con escrituras.

## Borrado logico
`DELETE` sobre presupuestos y facturas asigna el estado inactivo; luego dejan de aparecer en listados/busquedas y
responden 404. El estado se configura con `ESTADO_INACTIVO_ID` o, si no, `ESTADO_INACTIVO_NOMBRE` (por defecto `Inactivo`,
se busca en la tabla `estados`).

## Reglas de negocio
- Cliente: CI unico (409); saldo actual <= limite de saldo (400).
- FacturaVenta: a credito requiere vencimiento; vencimiento >= emision; importe e IVA >= 0 (400).

## Configuracion
Todo se sobreescribe por variable de entorno (ver `application.properties`): puerto, contexto, base de datos,
Redis, TTL por seccion, JWT (`JWT_SECRET`, `JWT_ALGORITHM`, `JWT_ISSUER`, `JWT_ROLES_CLAIM`), roles, Eureka, Swagger,
paginacion y niveles de log. Sin `JWT_SECRET` la aplicacion no arranca.
