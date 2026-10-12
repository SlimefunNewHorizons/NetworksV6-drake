# NetworksExperimental-Drake

Modulo experimental auditado por Drake Labs. No es el JAR de produccion (`NetworksV6-Drake`).

- Nombre en servidor: `NetworksExperimental-Drake`
- Evita colision de comandos con el plugin de produccion durante pruebas.

Build local (requiere dependencias del foundry en `.m2`):

```bash
mvn -B -ntp -DskipTests -f experimental/pom.xml clean package
```
