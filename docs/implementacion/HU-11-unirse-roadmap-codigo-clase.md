# HU-11 · Unirse a un Roadmap con Código Alfanumérico de Clase Docente

## Historia de Usuario

> **Como usuario web, yo debería poder ingresar un código alfanumérico para unirme a un roadmap creado específicamente por mi profesor a fin de acceder exactamente a la ruta que corresponde a mi salón de clases.**

---

## Contexto Pedagógico y Escolar

En las instituciones educativas públicas del Perú, los docentes de primaria organizan experiencias de aprendizaje adaptadas a las características socioculturales de su aula o sección (ej. "3.° Grado B", "Escuela Multigrado Ayacucho"). A menudo, los profesores preparan o seleccionan una ruta curricular específica en RUPI y necesitan que sus estudiantes se vinculen de forma rápida y sencilla sin procesos de matrícula engorrosos ni configuraciones complejas.

Mediante un **código alfanumérico de clase** (por ejemplo: `WARI2026`, `AYACUCHO2026`, `PALABRAS2026`), el docente entrega a sus estudiantes un código corto y memorable que ellos pueden ingresar directamente en la plataforma desde cualquier pantalla (selector de grados, selector de materias o cabecera del mapa). Al ingresar el código:
1. El backend valida de forma segura su autenticidad, vigencia temporal y disponibilidad de cupos.
2. Si el estudiante no estaba matriculado, se le inscribe transaccionalmente en la versión de ruta correspondiente y se desbloquea su primera parada (`DISPONIBLE`).
3. Si ya estaba matriculado, la operación es idempotente y enlaza el código utilizado sin alterar su progreso existente.
4. La interfaz de usuario le da la bienvenida con voz de Rupi (`speakText`), festeja el éxito con animación táctil y lo redirige automáticamente al mapa interactivo de su salón.

---

## Tareas y Criterios de Aceptación Implementados

### 1. Formulario Web Táctil y Accesible (`JoinClassModal.tsx` & `JoinClassModal.css`)
- **Diseño artesanal escolar:** Diseñado bajo los estándares de `human-crafted-frontend` y `educational-game-ux`, erradicando el aspecto genérico de IA. Cuenta con botones con volumen físico (`:active` con desplazamiento y sombra reducida), bordes sólidos y la paleta cromática andina oficial:
  - Verde bosque (`#173e30`)
  - Verde hoja (`#31724b`)
  - Naranja Rupi (`#f95738`)
  - Amarillo sol/maíz (`#ffd26a`)
  - Azul cielo (`#246a91`)
- **Accesibilidad táctil:** Área de pulsación mínima de `48px x 48px`, tipografía Nunito de alto contraste y soporte de navegación completa por teclado con `:focus-visible` nítido.
- **Entrada amigable:**
  - Auto-conversión automática a mayúsculas para evitar confusiones de digitación en tabletas y celulares.
  - Validación en vivo con regex `[A-Za-z0-9_-]{3,30}`.
  - Atajos pedagógicos rápidos con chips de códigos sugeridos (`AYACUCHO2026`, `WARI2026`, `PALABRAS2026`, `CIENCIA2026`).
- **Locución interactiva de Rupi:** Rupi acompaña al niño verbalmente (`speakText`), explicando cómo ingresar el código y celebrando cuando se une a la clase con éxito.
- **Puntos de acceso en el Frontend:**
  - Botón táctil destacado en el encabezado global de la aplicación (`App.tsx`).
  - Botón interactivo "Tengo un código de mi profesor" en la pantalla de bienvenida/selección de grado (`GradeSelectView.tsx`).

### 2. Servicio Backend de Validación y Lógica de Negocio (`LearningRouteService.java`)
- **Endpoint seguro:** `POST /api/v1/student/learning-routes/join-by-code` (requiere sesión autenticada de estudiante, cookies seguras `rupi_session` y verificación de origen CSRF).
- **Normalización y defensa en profundidad:** Limpieza de espacios, mayúsculas y verificación estricta contra inyecciones y caracteres inválidos mediante regex.
- **Validaciones temporales y de vigencia:**
  - Comprobación de existencia del código en `aprendizaje_codigo_acceso_ruta`.
  - Validación de estado revocado (`revocado_en IS NOT NULL` -> `400 Bad Request`).
  - Validación de fecha de inicio (`valido_desde > NOW()` -> `400 Bad Request`).
  - Validación de fecha de caducidad (`valido_hasta < NOW()` -> `400 Bad Request`).
  - Validación de cupos máximos de alumnos (`maximo_usos` alcanzado -> `400 Bad Request`).
  - Verificación de estado de la ruta vinculada (`PUBLICADO`).
- **Manejo de errores formativos:** Mensajes en formato RFC-7807 (`ProblemDetail`) claros y pedagógicos diseñados para niños (ej. *"El código de clase ya venció. Pídele a tu profesor un código actualizado."*).

### 3. Actualización Transaccional en Base de Datos (`LearningRouteRepository.java`)
- **Principio de Escritor Único (Single Writer):** Spring Boot gestiona exclusivamente las tablas `aprendizaje_*`.
- **Cifrado unidireccional y búsqueda:** Búsqueda mediante hash SHA-256 binario (`BINARY(32)`):
  ```sql
  WHERE car.hash_codigo = UNHEX(SHA2(:code, 256))
  ```
- **Idempotencia y deduplicación:**
  - Si el alumno ya está matriculado (`aprendizaje_inscripcion_ruta`), se vincula el `codigo_acceso_id` sin duplicar matrículas ni reiniciar su avance, retornando `alreadyEnrolled: true`.
  - Si es su primer ingreso, se crea la inscripción en estado `ACTIVO` con `UUID` generado y se inicializa el primer nodo en estado `DISPONIBLE`.

### 4. Notificaciones, Redirección y Experiencia de Usuario
- **Cliente API Frontend:** Función `joinRouteByCode(code: string)` en [`learningRoutesApi.ts`](../../frontend/web-react/src/services/learningRoutesApi.ts).
- **Redirección reactiva:** Al confirmarse la vinculación, el frontend actualiza el grado escolar activo, selecciona la materia correspondiente, cierra el modal y redirige al estudiante directamente al mapa interactivo de la ruta de su clase.

---

## Estructura de Base de Datos (MySQL 8.4)

### Migración [`V11__codigos_acceso_clase_hu11.sql`](../../database/mysql/migrations/V11__codigos_acceso_clase_hu11.sql)

```sql
CREATE TABLE IF NOT EXISTS aprendizaje_codigo_acceso_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  hash_codigo BINARY(32) NOT NULL UNIQUE,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  valido_desde DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  valido_hasta DATETIME(6) NULL,
  maximo_usos INT UNSIGNED NULL,
  revocado_en DATETIME(6) NULL,
  FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (valido_hasta IS NULL OR valido_hasta > valido_desde),
  CHECK (maximo_usos IS NULL OR maximo_usos > 0)
) ENGINE=InnoDB;
```

---

## Verificación y Calidad

### Pruebas Unitarias de Backend (JUnit 5 & Mockito)
- **47 de 47 pruebas superadas** en `backend/api-spring-boot` (`mvn test`).
- 7 nuevas pruebas especializadas en `LearningRouteServiceTest.java`:
  - `joinByCodeSuccessNewEnrollment`
  - `joinByCodeAlreadyEnrolledIsIdempotent`
  - `joinByCodeInvalidFormatThrowsBadRequest`
  - `joinByCodeNotFoundThrowsNotFound`
  - `joinByCodeExpiredThrowsBadRequest`
  - `joinByCodeRevokedThrowsBadRequest`
  - `joinByCodeMaxUsesReachedThrowsBadRequest`

### Pruebas Unitarias de Frontend (Node Test Runner)
- **12 de 12 pruebas superadas** (`node --experimental-strip-types --test frontend/web-react/tests/joinCode.test.ts`):
  - Normalización de código en mayúsculas y eliminación de espacios.
  - Validación de longitud (3 a 30 caracteres).
  - Mapeo de áreas curriculares devueltas a cursos del frontend.
  - Extracción de errores RFC-7807 del backend.

### Compilación y Linter Frontend
- `npm run build`: 0 errores en TypeScript y Vite build exitoso.
- `npm run lint`: 0 advertencias y 0 errores en ESLint.

### Script de Integración E2E PowerShell
- [`scripts/Verificar-HU11.ps1`](../../scripts/Verificar-HU11.ps1) ejecuta 10 escenarios de validación de integración:
  1. Protección anónima (401 Unauthorized).
  2. Autenticación de sesión de estudiante.
  3. Rechazo de código en blanco (400 Bad Request).
  4. Rechazo de caracteres no permitidos (400 Bad Request).
  5. Código inexistente (404 Not Found).
  6. Código expirado (400 Bad Request).
  7. Código revocado (400 Bad Request).
  8. Vinculación exitosa a clase activa con código `WARI2026`.
  9. Idempotencia al reintentar con el mismo código (`alreadyEnrolled: true`).
  10. Acceso al mapa interactivo de la ruta vinculada con sus paradas.
