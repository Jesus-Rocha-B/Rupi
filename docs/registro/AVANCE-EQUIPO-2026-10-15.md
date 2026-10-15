# Avance del equipo · 15 de octubre de 2026

## Actualización de la copia local

- Repositorio: https://github.com/Jesus-Rocha-B/Rupi
- Rama: `main`.
- Se sincronizó por avance directo desde `8fbaefe` hasta `71ed612`, sin conflictos ni cambios locales previos.
- Novedad remota: un commit de **Jesus-Rocha-B**, `71ed612`, del 15 de octubre.
- Ese commit modifica 381 archivos. Excluyendo `.agents`, son 34 archivos con 5575 inserciones y 808 eliminaciones. Gran parte del volumen total corresponde a herramientas/skills, no a funciones nuevas de Rupi.

## Aportes por integrante según Git

La atribución corresponde al autor registrado en los commits, no a una estimación del esfuerzo individual. El rango resumido es `48a9357..71ed612` (21 commits); no representa toda la historia del proyecto.

| Autor en Git | Commits en el rango | Aportes registrados |
|---|---:|---|
| Jesus-Rocha-B | 9 | Reparaciones de creación de rutas e ingreso a nodos; tildes; catálogo Oficial/Comunidad; navegación, perfil y avatar; separación de grado y ruta; contextos Costa/Sierra/Selva; pantalla de lección con videos; panel docente ampliado. |
| ErickGamarra | 6 | Correcciones de sesión y carga de rutas; eliminación de inscripción automática; asignación docente demo; materiales sin archivos huérfanos; progreso faltante; validaciones de autoría/enlaces; pausa y calendario institucional; documentación de actualización. |
| JContrerasC24 | 6 | HU-12 materiales MINEDU y descarga por nodo/sin conexión; HU-13 calendarización; HU-14 recordatorios sin duplicados; HU-16 refuerzos; HU-17 reportes docentes y CSV. |

Referencias para revisar los aportes:

- Jesús: `42eb482`, `b958876`, `8cabcb4`, `ee715b7`, `ce069d7`, `9cf375f`, `e3f4a5b`, `75a62b4`, `71ed612`.
- Erick: `0f8b83a`, `5719fb8`, `5c3c262`, `c5e186b`, `d57d720`, `fd16b62`.
- José (JContrerasC24): `85ac0b1`, `2334e5b`, `2422f7c`, `d37efbf`, `13aa934`, `8fbaefe`.

## Novedades de 71ed612 revisadas en el código

- Editor docente de paradas con detalle y actualización mediante GET/PUT `/api/v1/teacher/learning-routes/{routeId}/nodes/{nodeId}`.
- Edición de preguntas, opciones, instrucciones, videos y fechas de disponibilidad/bloqueo.
- La API de actividades comprueba las fechas de acceso de los nodos.
- Rediseño del panel docente y ajustes de selección de grado, catálogo, mapa, perfil y pantalla de lección.
- Ajustes visuales de materiales descargables, refuerzos HU-16 y reportes HU-17.
- Catálogo de videos educativos y scripts de apoyo para videos.
- Migración V19 con datos comunitarios del docente demo y códigos de acceso.
- Incorporación de skills de diseño, animación y otras herramientas en `.agents`.

Este registro describe cambios revisados; no afirma una validación manual completa de cada función nueva.

## Ajustes de integración realizados al actualizar

1. **V20: columnas de fechas faltantes.** El código nuevo consultaba `disponible_desde` y `bloqueado_desde` en `aprendizaje_nodo_ruta`, pero no había una migración ni definición en `schema.sql`. Se agregan columnas UTC `DATETIME(6)` opcionales, de forma idempotente. Se aplicó en la base local después de crear un respaldo en `.local`.
2. **Arranque en Windows.** Se corrigen las comillas de rutas con espacios y se utiliza la configuración heredada del entorno, sin colocar contraseñas ni tokens en argumentos de Java.
3. **Lint.** Se corrigen cinco errores: dos asignaciones iniciales que siempre se sobrescribían, una conversión booleana redundante y dos propiedades desestructuradas sin uso. Sin alterar el flujo funcional.

## Pendientes detectados

- **V19 no aplicada a esta base:** ninguna de las dos versiones de ruta requeridas (`41581fe2-4b60-4a7d-8d3e-d2224780fbae`, `c51711e8-4ed9-49ca-8efd-f9bb88846b97`) existe localmente. La migración actualiza padres que presupone existentes e inserta hijos; ejecutarla aquí produciría errores de integridad. Debe hacerse portable o trasladarse el conjunto completo de datos antes de aplicarla.
- V19 contiene identificadores con prefijos `en`, `ep` y `er`, que no son UUID hexadecimales válidos para los endpoints que utilizan `UUID`. Revisar esos datos antes de desplegarlos.
- **Historial de respuestas:** `TeacherRouteRepository.updateQuestionsForActivity` elimina respuestas/opciones anteriores al reemplazar preguntas. Esto puede afectar la evidencia de HU-16/HU-17. Se registra para corregir con versionado/conservación de preguntas evaluadas; no se ejecutó esa operación sobre los resultados locales.
- La copia local conserva su catálogo reducido anterior. Sincronizar Git no traslada automáticamente la base de datos de otro compañero.

## Verificación de esta actualización

- API: compilación y empaquetado correctos. 168 pruebas descubiertas: 143 ejecutadas sin fallos y 25 omitidas que requieren bases aisladas de integración.
- Frontend: build, lint y 21 pruebas correctos después de los ajustes.
- MySQL local iniciado; V20 aplicada y columnas verificadas.
- Web `http://localhost:5173/` y API a través del proxy `/api/v1/auth/options`: HTTP 200.
- No se hicieron pruebas de extremo a extremo de todo el nuevo editor ni se ejecutó el script de siembra de videos.

## Terminal local

Directorio del proyecto: `C:\Users\Usuario 1\Proyectos\Rupi`.

```powershell
Set-Location -LiteralPath 'C:\Users\Usuario 1\Proyectos\Rupi'
git status
git log -15 --oneline
.\Iniciar-Rupi.ps1
```

Para otra copia: sincronizar `main`, aplicar `database/mysql/migrations/V20__fechas_disponibilidad_nodos.sql` con la cuenta administrativa de MySQL, compilar la API y reiniciarla. V19 requiere resolver primero las dependencias mencionadas; no borrar la base para incorporarla.
