# Estado actual · RUPI

Actualizado: 2026-10-07 · rama `main` · último commit `6b0049b`.

## Historias de usuario

| HU | Resultado | Estado |
|---|---|---|
| HU-01 | Mapa interactivo con datos reales por estudiante | Completada |
| HU-02 | Clic en un nivel: abrir actividad y marcarla EN_CURSO | Implementada y verificada localmente |
| HU-03 | Regresar al punto exacto del mapa | Implementada y verificada localmente |
| HU-04 | Ruta agrupada por unidades MINEDU | **Pendiente, es la siguiente** |

«Verificada localmente» no sustituye la aceptación del docente ni la validación pedagógica. No marcar la entrega como aceptada sin revisar con el equipo los criterios de S07.

## Trabajo sin commitear (al 2026-10-07)

Soporte de entorno Linux (probado en Fedora): `iniciar-rupi.sh`, `detener-rupi.sh`, `LEEME-LOCAL-LINUX.md`, `docs/implementacion/entorno-linux.md`. Además, esta carpeta `docs/registro/`.

Comprobado el 2026-10-07 en Fedora 44: `./iniciar-rupi.sh` levanta MySQL, API y web, y `verify_learning_flow.py` pasa sus 23 comprobaciones. Sin probar: otras distribuciones, ARM, y `mvn test`, `npm run build` y `npm run lint` en Linux.

## Pendiente

**HU-04** (detalle en `docs/implementacion/README.md`):
- [ ] Catálogo vigente de prueba y vínculos curriculares (`aprendizaje_nodo_ruta.unidad_id`, `aprendizaje_actividad_unidad`).
- [ ] Validar en el backend grado, área, versión de catálogo y pertenencia de unidades.
- [ ] Agrupar y ordenar la ruta por unidad en API y React sin perder el progreso individual.

**Fuera del alcance actual** (otras historias): finalización de actividades, calificación, XP, desbloqueo del siguiente nodo, intentos de evaluación, Django admin y la app Kotlin.

**Deuda técnica conocida:**
- Spring escribe tablas `identidad_*` (login), contra la regla de escritor único. Hay que reconciliarlo con Django.
- Elegir un único mecanismo de migraciones y fijar el orden Django/Spring.
- Privacidad y acceso para menores antes de publicar.
- La cuenta demo y los datos de demostración no son para despliegue.
- Las hojas de estilo son muy grandes (`App.css` y `AuthViews.css`, unas 2300 líneas cada una) y podrían dividirse.
- `frontend/web-react/src/features/roadmap/lessons.ts` es un resto del origen estático anterior; comprobar si aún se usa.

## Última evidencia de pruebas (2026-10-06, Windows)

28 pruebas Java y 2 de selección de rutas aprobadas. Build de producción y ESLint verificados. `scripts/verify_learning_flow.py` ejecutó 23 escenarios HTTP contra MySQL real. Falta repetir en Fedora.
