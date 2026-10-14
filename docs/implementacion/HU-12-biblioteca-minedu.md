# HU-12 · Biblioteca MINEDU y repaso descargable

Fecha: 13/10/2026. Base de implementación: `48a9357`.

## Resultado

El panel «Llévate tu unidad» conserva la descarga del roadmap y agrega «Tu biblioteca MINEDU». El backend selecciona la referencia por el **grado y área reales de la ruta autorizada**, nunca por el primer resultado del catálogo. Hay 18 referencias (6 grados × 3 materias) y 36 ejercicios originales de repaso (2 por combinación). Las materias fuera de esa selección no reciben material de otro curso.

La biblioteca es una selección de publicaciones existentes del repositorio institucional; no es una conexión a una API curricular del MINEDU ni una afirmación de que todas sean la dotación más reciente. Las fechas de edición se muestran en pantalla. Ciencia de sexto usa una carpeta integrada con una sección de biodiversidad, identificada como tal.

## Qué se descarga

- El ZIP conserva `guia-de-estudio.pdf`, los materiales locales disponibles del docente, `contenido.json`, `manifest.json` y `LEEME.txt`.
- Añade `repaso-rupi.pdf`: ejercicios originales de Rupi, espacio para responder y una página separada de respuestas. Es repaso general del grado, no evaluación específica de la unidad ni material oficial del MINEDU. No cambia notas, progreso, XP ni estados del roadmap.
- Añade `fuentes-minedu.html`: referencias oficiales con enlaces clicables y advertencia de que requieren internet.
- Los libros oficiales **se descargan por separado en su ficha MINEDU**. No se copian ni redistribuyen dentro del ZIP: los PDF consultados incluyen avisos de derechos reservados aunque algunas fichas muestren licencias abiertas. No se reproducen sus ejercicios como contenido propio.
- La guía, los adjuntos incluidos y el repaso sí funcionan offline. Las fuentes externas deben guardarse antes de desconectarse. La disponibilidad del sitio oficial depende del MINEDU; puede solicitar verificación de seguridad.

## Fuentes seleccionadas

Los títulos/grados se contrastaron con fichas y resultados indexados del repositorio oficial el 13/10/2026. No se garantiza disponibilidad futura ni revisión pedagógica docente de los ejercicios originales.

| Grado | Área | Recurso | Año |
|---|---|---|---|
| 1.° | Comunicación | [Cuadernillo de Comunicación 1](https://repositorio.minedu.gob.pe/handle/20.500.12799/11801) | 2025 |
| 1.° | Ciencia y Tecnología | [Ciencia y Tecnología 1: cuaderno de autoaprendizaje](https://repositorio.minedu.gob.pe/handle/20.500.12799/8321) | 2021 |
| 1.° | Matemática | [Cuadernillo de Matemática 1](https://repositorio.minedu.gob.pe/handle/20.500.12799/10079) | 2023 |
| 2.° | Comunicación | [Cuadernillo de Comunicación 2](https://repositorio.minedu.gob.pe/handle/20.500.12799/11802) | 2025 |
| 2.° | Ciencia y Tecnología | [Ciencia y Tecnología 2: cuaderno de autoaprendizaje](https://repositorio.minedu.gob.pe/handle/20.500.12799/8322) | 2021 |
| 2.° | Matemática | [Cuadernillo de Matemática 2](https://repositorio.minedu.gob.pe/handle/20.500.12799/11800) | 2025 |
| 3.° | Comunicación | [Cuadernillo de Comunicación 3](https://repositorio.minedu.gob.pe/handle/20.500.12799/10075) | 2023 |
| 3.° | Ciencia y Tecnología | [Ciencia y Tecnología 3: cuaderno de autoaprendizaje](https://repositorio.minedu.gob.pe/handle/20.500.12799/8323) | 2021 |
| 3.° | Matemática | [Cuadernillo de Matemática 3](https://repositorio.minedu.gob.pe/handle/20.500.12799/10081) | 2023 |
| 4.° | Comunicación | [Cuadernillo de Comunicación 4](https://repositorio.minedu.gob.pe/handle/20.500.12799/11294) | 2024 |
| 4.° | Ciencia y Tecnología | [Ciencia y Tecnología 4: cuaderno de autoaprendizaje](https://repositorio.minedu.gob.pe/handle/20.500.12799/8324) | 2021 |
| 4.° | Matemática | [Cuadernillo de Matemática 4](https://repositorio.minedu.gob.pe/handle/20.500.12799/10082) | 2023 |
| 5.° | Comunicación | [Cuadernillo de Comunicación 5](https://repositorio.minedu.gob.pe/handle/20.500.12799/11295) | 2024 |
| 5.° | Ciencia y Tecnología | [Ciencia y Tecnología 5: cuaderno de autoaprendizaje](https://repositorio.minedu.gob.pe/handle/20.500.12799/8325) | 2021 |
| 5.° | Matemática | [Cuadernillo de Matemática 5](https://repositorio.minedu.gob.pe/handle/20.500.12799/8877) | 2022 |
| 6.° | Comunicación | [Cuadernillo de Comunicación 6](https://repositorio.minedu.gob.pe/handle/20.500.12799/11296) | 2024 |
| 6.° | Ciencia y Tecnología | [Carpeta de Recuperación A: sexto grado de Primaria](https://repositorio.minedu.gob.pe/handle/20.500.12799/7034) | 2021 |
| 6.° | Matemática | [Cuadernillo de Matemática 6](https://repositorio.minedu.gob.pe/handle/20.500.12799/8878) | 2022 |

## Contrato e integración

`GET /api/v1/student/learning-routes/{versionRouteId}/study-resources` devuelve las referencias de la ruta. Reutiliza `LearningRouteService.getForStudent` para comprobar inscripción y publicación; el filtro de sesión sigue protegiendo `/student/**`. Una ruta no autorizada no entrega su biblioteca.

El catálogo se mantiene en `backend/api-spring-boot/src/main/resources/study/primary-resources.json`, con procedencia y ejercicios separados. No se modifican tablas `curriculo_*` ni se introduce una migración: Django sigue siendo el propietario del catálogo curricular. Esta selección editorial pertenece al empaquetado de HU-12.

`StudyPackageModel.Unit` incorpora las referencias y ejercicios; estos entran en la clave de caché. La versión del formato sube a 2. Cada archivo adicional figura con tamaño y SHA-256 en el manifest. El empaquetador no necesita acceso a internet para generar el repaso.

## Verificación

- 91 pruebas Java (incluyen 6 nuevas de catálogo, autorización, selección por grado, caché, PDF y ZIP).
- Los 18 PDF de repaso generan dos páginas; preguntas y respuestas quedan separadas. Los hashes/tamaños de los ZIP coinciden con el manifest.
- 17 pruebas Node, compilación TypeScript/Vite y ESLint.
- Navegador Edge con respuestas controladas: 1280 y 375 px, enlace del grado correcto, botón de al menos 48 px, sin desborde del panel, descarga por teclado, hash válido y recuperación del error de biblioteca. Estas pruebas de interfaz no sustituyen la prueba API/MySQL.

- API Spring Boot y MySQL 8.4 aislados: `verify_hu12_resources.py` verificó las **18 combinaciones** y los rechazos 401/404. Se usaron schema, V04, semillas y V06-V11; no se aplicó V12 ni se cambió la base del equipo.
- `Verificar-HU12-Descarga.ps1` en **Windows PowerShell 5.1**: 10 comprobaciones correctas, 0 fallas (ruta con una unidad), incluida segunda descarga desde caché.

```powershell
python scripts/verify_hu12_resources.py --api http://127.0.0.1:8081
powershell.exe -NoProfile -File scripts/Verificar-HU12-Descarga.ps1
```

## Cómo probar

1. Arrancar la API y web con la base del equipo correctamente preparada.
2. Ingresar como estudiante e ir a una ruta inscrita de Matemática, Comunicación o Ciencia y Tecnología de 1.° a 6.°.
3. En «Llévate tu unidad», comprobar que la biblioteca coincide con el grado/materia de esa ruta.
4. Descargar la unidad, extraer el ZIP y abrir `repaso-rupi.pdf` sin conexión.
5. Confirmar que las respuestas están en la segunda página y que las fuentes indican qué requiere internet.
6. Cambiar de grado/materia y comprobar que cambia el recurso y los ejercicios.

Los problemas previos de V12, autoría docente y carga de materiales quedan fuera de este cambio, a cargo del equipo.


## Descarga por nodo y consulta sin conexión

La HU-12 permite descargar una parada o una unidad completa desde «Llévate tus materiales», debajo del mapa. El selector empieza en «Una parada del mapa». Se conserva la paleta y los botones táctiles de Rupi.

Endpoint: `GET /api/v1/student/learning-routes/{versionRouteId}/nodes/{nodeId}/package`.
Exige sesión e inscripción en una ruta publicada, valida que el nodo pertenezca a esa ruta y consulta únicamente su actividad publicada. Devuelve 401 sin sesión, 404 ante ruta/nodo ajeno y 409 sin contenido publicado. Como en la descarga por unidad, se permite estudiar de antemano las paradas publicadas; descargar no desbloquea ni completa actividades.

El ZIP incluye `ABRIR.html`, una guía PDF, preguntas con respuestas para revisar, adjuntos locales disponibles, repaso Rupi por grado, referencias oficiales, instrucciones y manifest con hashes. Tras extraer todo el ZIP, `ABRIR.html` funciona directamente en el navegador sin servidor, sesión, scripts ni peticiones de red. No es un modo offline de toda la aplicación y no sincroniza respuestas ni progreso. Los adjuntos ausentes o dañados aparecen como no incluidos en el lector, las instrucciones y el manifest. Dos adjuntos con el mismo nombre conservan archivos separados.

Los enlaces web y los libros externos del MINEDU no se convierten en archivos locales automáticamente: la biblioteca indica que el PDF oficial debe guardarse por separado antes de desconectarse. Los ejercicios incluidos son originales de Rupi.

Validación de esta ampliación: pruebas de servicio (aislamiento de nodo, inscripción, publicación y caché), controlador HTTP (401, ZIP y cabeceras), lector con contenido escapado, advertencia de archivos faltantes, hashes de paquete, compilación y lint del frontend. Prueba visual aislada con respuestas HTTP simuladas en 1280 y 375 px, descarga por teclado, selector y reintento; apertura de `ABRIR.html` con la red del navegador desactivada. El script `verify_hu12_resources.py` permite repetir la comprobación de unidades y nodos contra la API local; esta ampliación no modifica la base de datos ni corrige los problemas de autoría reportados por separado.
