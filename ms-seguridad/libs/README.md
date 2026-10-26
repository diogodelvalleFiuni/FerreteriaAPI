# libs

`commons-0.0.1-SNAPSHOT.jar` es el MISMO JAR que usan ms-inventario y ms-cache-redis (entidades
`com.ferreteria.entity.*`, DTOs `com.ferreteria.commons.dto.*`, mappers y changelogs Liquibase).
El pom lo toma directo de esta carpeta (`scope=system`), sin instalar nada en `.m2`.

Este servicio usa de commons: `Usuario`, `Rol` y `Estado` (entidades), `UsuarioDTO` / `RolDTO` (contrato de
`POST /api/usuarios`), `UsuarioMapper` y `ApiErrorDTO`. Las tablas `usuarios`, `roles` y `usuarios_roles`
las crea commons (changelogs 019-021); este servicio no toca el esquema.

Para actualizar commons: reemplazar el JAR conservando el nombre.
