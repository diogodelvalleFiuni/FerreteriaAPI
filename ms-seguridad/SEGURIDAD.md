# Seguridad (ms-seguridad)

## Endpoints
| Metodo y ruta | Acceso | Que hace |
|---|---|---|
| `POST /api/auth/login` | publico | `{username,password}` -> access token + refresh token (limite de intentos: 429 con `Retry-After`) |
| `POST /api/auth/refresh` | publico | `{refreshToken}` -> access token nuevo (relee usuario y roles de la base) |
| `GET /api/auth/me` | access token | `{username, roles}` del token |
| `POST /api/usuarios` | solo ADMIN | alta de usuario: `{"username":"juan","password":"...","roles":[{"nombre":"VENDEDOR"}]}` (contrato `UsuarioDTO` de commons) |

El access token se manda como `Authorization: Bearer <token>` a ms-seguridad, ms-inventario y ms-cache-redis.
El refresh token SOLO sirve en `/api/auth/refresh`: va firmado con otra clave, asi que los demas servicios lo rechazan.

## Variables de entorno
| Variable | Quien la usa | Nota |
|---|---|---|
| `JWT_SECRET` | los 3 servicios | MISMA en los tres, minimo 32 bytes |
| `JWT_REFRESH_SECRET` | solo ms-seguridad | obligatoria, minimo 32 bytes y distinta de `JWT_SECRET` |
| `JWT_EXPIRATION_MINUTES` / `JWT_REFRESH_EXPIRATION_DAYS` | solo ms-seguridad | por defecto 60 min / 7 dias |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` | solo ms-seguridad | por defecto 5 fallos -> 15 min de bloqueo (por IP + usuario) |
| `BOOTSTRAP_ADMIN_USER` / `BOOTSTRAP_ADMIN_PASSWORD` | solo ms-seguridad | crea el primer ADMIN si la tabla `usuarios` esta vacia; quitarlas despues |
| `FORWARD_HEADERS_STRATEGY` | solo ms-seguridad | `framework` si va detras del gateway y el puerto no es publico (para que el limite use la IP real) |

## Primer arranque
Las tablas `usuarios`, `roles` y `usuarios_roles` las crea commons (changelogs 019-021). Con `BOOTSTRAP_ADMIN_USER` y
`BOOTSTRAP_ADMIN_PASSWORD` se crea el rol `ADMIN` (si falta) y el primer usuario. Los demas roles (ej. `VENDEDOR`)
tienen que existir y estar activos en la tabla `roles`: el alta de usuarios no crea roles.

## Reglas por rol en los otros servicios
- ms-inventario: `DELETE /api/**` solo ADMIN; el resto, cualquier usuario autenticado.
- ms-cache-redis: `DELETE /api/cache` (vaciar por prefijo) solo ADMIN; el resto, cualquier usuario autenticado.
