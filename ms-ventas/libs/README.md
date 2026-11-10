# libs

`commons-0.0.1-SNAPSHOT.jar`: entidades (`com.ferreteria.entity.*`) y DTOs (`com.ferreteria.commons.dto.*`) del
proyecto commons. **No se modifica aca.**

Este es el commons NUEVO (el mismo que usan ms-inventario y ms-seguridad): trae `Usuario`/`Rol`, las fechas de
`Transferencia` como `LocalDateTime` y un `ApiErrorDTO` con `timestamp` en `LocalDateTime`. ms-ventas necesita esta
version: con el JAR viejo (timestamp `OffsetDateTime`) no compila el manejo de errores de seguridad.

El `pom.xml` lo toma directo de esta carpeta (`scope=system`), sin instalar nada en `.m2`.
Para actualizar commons: generar `./mvnw clean package` en el proyecto commons y reemplazar el JAR
(el normal, no el `-exec`) conservando el nombre.
