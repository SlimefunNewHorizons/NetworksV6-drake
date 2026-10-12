## Resumen
- Describe brevemente qué problema resuelve este PR.
- Incluye el alcance técnico del cambio.

## Checklist
- [ ] Probado localmente (build y arranque)
- [ ] Sin breaking changes no documentados
- [ ] README/Docs actualizados si aplica
- [ ] Compatibilidad validada para `main`

## Test plan
- [ ] `mvn -DskipTests clean package` o `./gradlew build`
- [ ] Arranque en Paper/Purpur 1.21.x
- [ ] Revisión de `logs/latest.log` sin errores nuevos

## Riesgos
- Impacto esperado y plan de rollback.
