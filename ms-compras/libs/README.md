# libs

`commons-0.0.1-SNAPSHOT.jar` es el JAR de la Entrega #1 (entidades `com.ferreteria.entity.*`
y DTOs `com.ferreteria.commons.dto.*`). **No se modifica.**

El `pom.xml` lo toma directo de esta carpeta (`scope=system`), sin instalar nada en `.m2`.
Para actualizar commons: reemplazar el JAR conservando el nombre.

Nota: este JAR es el mismo que usan ms-inventario y ms-seguridad (incluye usuarios/roles y `ApiErrorDTO` con `LocalDateTime`).
