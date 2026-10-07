# Contexto del proyecto · RUPI

## Qué es

Plataforma web (y luego móvil) de aprendizaje para estudiantes de primaria de colegios públicos del Perú, de 6 a 12 años. El gallito de las rocas (*Rupicola peruvianus*), Rupi, guía rutas interactivas ligadas al currículo MINEDU y al contexto cultural peruano. La ruta de demostración está ambientada en Ayacucho.

## Arquitectura

```text
frontend/web-react ──> backend/api-spring-boot ──> MySQL `rupi` (8.4, InnoDB, utf8mb4)
(React, Vite)          (Spring Boot, Java 21)       ▲
                                                    │
                       backend/admin-django ────────┘ (solo esqueleto: curriculo/models.py y admin.py)
frontend/mobile-kotlin: solo README, sin código.
```

- React nunca accede a MySQL ni envía `estudianteId`; Spring determina la identidad desde la cookie de sesión.
- **Escritor único por dominio** (regla de `AGENTS.md`): Spring escribe `escuela_*`, `aprendizaje_*`, `evaluacion_*`, `gamificacion_*`. Django escribe `identidad_*` y `curriculo_*`.
- Excepción de desarrollo documentada: Spring escribe `identidad_sesion_usuario` e `identidad_intento_acceso` por el login. Está pendiente reconciliarlo con Django.
- Invariantes: el porcentaje de avance nunca se guarda, se calcula al consultar. IDs `CHAR(36)` (UUID). Fechas `DATETIME(6)` en UTC.

## Reglas de trabajo (de `AGENTS.md`)

- Prohibido el aspecto genérico de IA: sin gradientes morado/azul ni halos difusos, y sin copys corporativos vacíos.
- Paleta: verde bosque `#173e30`, verde hoja `#31724b`, naranja Rupi `#f95738`, amarillo maíz `#ffd26a`, azul cielo `#246a91`.
- Botones con volumen físico; en `:active` bajan y reducen la sombra.
- Objetivos táctiles de al menos 48×48 px, contraste WCAG AA o mejor y `:focus-visible` nítido.
- Antes de crear UI, consultar `.agents/skills/human-crafted-frontend/`. Para mecánicas de gamificación, `.agents/skills/educational-game-ux/`.
- Scripts de PowerShell compatibles con Windows PowerShell 5.1 y con PowerShell 7+.
- Idioma del producto y de la documentación: español.

## Entorno local

Hay dos equivalentes. Windows: `Iniciar-Rupi.cmd` o `.ps1`, con credenciales en `.local/env.ps1`. Linux: `./iniciar-rupi.sh` y `./detener-rupi.sh`, con credenciales en `.local/env.sh`.

- Puertos: web 5173, API 8081, MySQL propio de Rupi 3307. El MariaDB del sistema (3306) no se toca.
- Linux (Fedora): Temurin 21 en `~/jdks/jdk-21.0.12.1+1`, fijado por `.local/env.sh`. MySQL portable 8.4.11 en `.local/`. No se usa `sudo`.
- `.local/` está ignorado por git. No borrar `.local/mysql-data`, porque conserva el progreso.
- Cuenta demo ficticia: `estudiante.demo` / `123456`. Solo tiene Matemática de 2.º de primaria. No es apta para despliegue público.
- Recompilar la API: `./iniciar-rupi.sh --recompilar` con la API detenida.
- Detalle en `LEEME-LOCAL.md` (Windows), `LEEME-LOCAL-LINUX.md` y `docs/implementacion/entorno-linux.md`.

### Verificaciones

```bash
JAVA_HOME=~/jdks/jdk-21.0.12.1+1 mvn -f backend/api-spring-boot/pom.xml test
npm run build && npm run lint
node --experimental-strip-types --test frontend/web-react/tests/routeSelection.test.ts
python3 -I scripts/verify_learning_flow.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
```

En Linux, para consultar la base de Rupi hay que usar el cliente de `.local/mysql-8.4.*/bin/mysql`; el `mysql` del sistema es el de MariaDB.

## Decisiones vigentes

- El servidor es la fuente de verdad del progreso; `localStorage` solo guarda preferencias de interfaz no sensibles.
- No se crean tablas nuevas sin necesidad demostrada; el modelo de 77 tablas ya cubre las 60 historias del backlog.
- Las rutas disponibles salen de la inscripción activa o completada del estudiante. No se inventan rutas ni avance para materias sin contenido.
- El contenido de actividades se renderiza sin interpretar HTML.
- Las migraciones se versionan en `database/mysql/migrations/` (V04–V06). `seed-hu01.sql` actualiza estados de demostración y no se vuelve a ejecutar sobre progreso real.
- Falta elegir el mecanismo único de migraciones entre Django y Spring, y resolver privacidad y acceso de menores antes de publicar.

## Documentación existente (no duplicada aquí)

- `docs/implementacion/README.md`: plan de HU-01 a HU-04 y lista de avance.
- `docs/implementacion/HU-01-mapa-interactivo.md` y `HU-01-verificacion-local.md`
- `docs/implementacion/HU-02-HU-03.md`: contratos, migraciones y evidencia.
- `database/mysql/README.md`, `modelo.md`, `schema.sql`, `rupi.dbml`
- `backend/api-spring-boot/README.md`
