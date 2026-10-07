# Estado actual · RUPI

Actualizado: 2026-10-07 · rama `main` · HU-04 incluida en el último commit (`git log -1`).

## Historias de usuario

| HU | Resultado | Estado |
|---|---|---|
| HU-01 | Mapa interactivo con datos reales por estudiante | Completada |
| HU-02 | Clic en un nivel: abrir actividad y marcarla EN_CURSO | Implementada y verificada localmente |
| HU-03 | Regresar al punto exacto del mapa | Implementada y verificada localmente |
| HU-04 | Ruta agrupada por unidades MINEDU | Implementada y revisada en navegador. Competencias, estándares y desempeños son oficiales; las unidades son de demostración |

«Verificada localmente» no sustituye la aceptación del docente ni la validación pedagógica. No marcar la entrega como aceptada sin revisar con el equipo los criterios de S07.

## Trabajo sin commitear

Ninguno al cerrar HU-04. Detalle de lo que cambió en `docs/registro/CHANGELOG.md`.

## Pendiente

**HU-04** (detalle en `docs/implementacion/README.md`):
- [x] Tarea 4: `verify_hu04_content.py` y `verify_hu04_pantalla.mjs` comparan base, API y pantalla con `scripts/fixtures/hu04-orden-esperado.json`.
- [x] Mapa revisado en Firefox (escritorio 1280 px y móvil 375 px) con Playwright.
- [ ] Reemplazar las unidades `DEMO-*` por la programación real cuando Django cargue el catálogo oficial (ver `docs/implementacion/HU-04-fuentes-curriculares.md`).
- [x] Contenido de las paradas 6–10 alineado con su título y con desempeños oficiales (V07, `seed-hu02.sql`). Sin validar con un docente.
- [ ] Seguir el proceso de actualización del CNEB (RM 393-2026-MINEDU).
- [ ] Confirmar con el equipo la regla de paradas sin unidad (hoy: grupo final «Otras paradas»).
- [ ] Si las paradas de una unidad no son contiguas, el mapa sigue el orden de la ruta y repite la banda; revisar si hace falta reordenar.

**Fuera del alcance actual** (otras historias): finalización de actividades, calificación, XP, desbloqueo del siguiente nodo, intentos de evaluación, Django admin y la app Kotlin.

**Deuda técnica conocida:**
- Spring escribe tablas `identidad_*` (login), contra la regla de escritor único. Hay que reconciliarlo con Django.
- Elegir un único mecanismo de migraciones y fijar el orden Django/Spring.
- Privacidad y acceso para menores antes de publicar.
- La cuenta demo y los datos de demostración no son para despliegue.
- Las hojas de estilo son muy grandes (`App.css` y `AuthViews.css`, unas 2300 líneas cada una) y podrían dividirse.
- `frontend/web-react/src/features/roadmap/lessons.ts` es un resto del origen estático anterior; comprobar si aún se usa.

## Última evidencia de pruebas (2026-10-07, Fedora)

36 pruebas Java y 5 de Node aprobadas. Build de producción y ESLint verificados. `scripts/verify_learning_flow.py`: 29 comprobaciones HTTP/MySQL. `scripts/verify_hu04_content.py`: 11 y `scripts/verify_hu04_pantalla.mjs`: 10 (Firefox, 1280 y 375 px). Detalle en `docs/implementacion/HU-04-ruta-por-unidades.md`.

El entorno Linux se probó en Fedora 44 (`./iniciar-rupi.sh` levanta MySQL, API y web). Sin probar: otras distribuciones y ARM.
