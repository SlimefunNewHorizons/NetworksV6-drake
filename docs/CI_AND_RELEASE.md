# CI, tests y releases

## Workflows

| Workflow | Trigger | Qué hace |
|----------|---------|----------|
| [Drake CI](../.github/workflows/drake-ci.yml) | push/PR a `main` | Clona foundry, `mvn test package`, sube artefacto SNAPSHOT |
| [Drake Release](../.github/workflows/drake-release.yml) | tag `v*` o manual | Tests + JAR renombrado + GitHub Release |

## Tests locales

```bash
mvn test
```

Incluye regresiones MockBukkit + Slimefun Drake:

- `NetworkStackAggregatorTest` — terracotta #226
- `StackUtilsTerracottaTest` — matching de colores

## Dependencias en CI

Los workflows compilan primero `DrakesCraft-Labs/drakes-slimefun-labs` (rama `main`) e instalan Dough, Slimefun, SefiLib e InfinityExpansion en `.m2`, luego construyen este repo.

Token: `GITHUB_TOKEN` con lectura de GitHub Packages (`github-drakes-foundry`).
