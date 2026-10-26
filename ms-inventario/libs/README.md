# libs

Aca va `commons-0.0.1-SNAPSHOT.jar` (entidades `com.ferreteria.entity.*` y DTOs
`com.ferreteria.commons.dto.*`).

**IMPORTANTE:** este proyecto necesita el commons NUEVO, con las fechas de `Transferencia`
como `LocalDateTime` / `date-time` (el JAR viejo las tenia como Long y no compila con este codigo).
Generarlo desde el proyecto commons con `./mvnw clean package` y copiar
`target/commons-0.0.1-SNAPSHOT.jar` (el JAR normal, no el `-exec`) en esta carpeta.

El `pom.xml` lo toma directo de aca (`scope=system`), sin instalar nada en `.m2`.
Para actualizar commons: reemplazar el JAR conservando el nombre.
