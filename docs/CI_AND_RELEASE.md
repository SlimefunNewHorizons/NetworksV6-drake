# CI, tests y releases

El proyecto se compila con **Gradle** (`build.gradle.kts`, wrapper incluido). No hay `pom.xml`.

## Workflows

| Workflow | Trigger | Qué hace |
|----------|---------|----------|
| [Drake CI](../.github/workflows/drake-ci.yml) | push/PR a `main` | `test`, `shadowJar`, verifica el jar y lo sube como artefacto. Un check aparte por cada API de Paper: `Paper API 1.21.11`, `Paper API 26.1`, `Paper API 26.2` |
| [Drake Release](../.github/workflows/drake-release.yml) | tag `v*` o manual | `test` + `paperCompat` + `shadowJar`, renombra el jar y crea el GitHub Release |
| [Publicar en Modrinth](../.github/workflows/modrinth-publish.yml) | push a `main` con cambios de código | `shadowJar` y publica `NetworksV6-Drake-*.jar` con versión `<version de build.gradle.kts>+<commit>` |

## Comandos locales

```bash
./gradlew test          # tests unitarios (JUnit 5)
./gradlew shadowJar     # jar del plugin: build/libs/NetworksV6-Drake-<version>.jar
./gradlew build         # todo lo anterior
./gradlew paperCompat   # compila contra paper-api 1.21.11, 26.1 y 26.2
```

En `build/libs` salen dos jars. El del plugin es **`NetworksV6-Drake-<version>.jar`** (shadowJar, con
las dependencias empaquetadas y relocalizadas). `NetworksExpansion-<version>.jar` es el jar normal de
Gradle y no funciona en un servidor.

## Compatibilidad con Paper

`./gradlew paperCompat` compila el mismo código contra otras versiones de la API de Paper (una tarea
por versión: `compileAgainstPaper1_21_11`, `compileAgainstPaper26_1`, `compileAgainstPaper26_2`). El
jar real se sigue compilando contra `paper-api 1.21.4`.

- La API de Paper 26.x está compilada para Java 25, así que estas tareas necesitan un **JDK 25**
  instalado (Gradle lo busca solo). La salida sigue siendo Java 21, igual que el jar.
- No cuelgan de `build` para no romper builds con solo Java 21 (JitPack).
- Es una prueba de código fuente: comprueba que todo lo que el plugin usa existe en esa API. No arranca
  un servidor.

Para añadir una versión: agregarla a `paperCompatTargets` en `build.gradle.kts` y a la matriz
`paper-compat` de `drake-ci.yml`.
