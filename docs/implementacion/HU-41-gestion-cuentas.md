# HU-41 · Altas, bajas y modificaciones de cuentas

Implementación: 28 de octubre de 2026 · commit `9f8a132`.

> Como administrador del sistema, quiero gestionar las modificaciones de cuentas y las altas y bajas de docentes y alumnos, para mantener un registro ordenado y depurado.

## Criterios de aceptación (definidos en la implementación)
- La administración crea cuentas de estudiante o docente con nombre, apellido, usuario y, si se desea, correo y nombre preferido.
- Cada cuenta nueva recibe una clave temporal fácil de dictar, que se muestra **una sola vez**.
- Se pueden buscar cuentas por nombre, usuario o correo y filtrar por rol y estado.
- Se pueden modificar los datos de una cuenta. Usuario y correo no se repiten.
- Dar de baja no borra nada: la cuenta queda `DESHABILITADO`, se cierra su sesión y no puede ingresar. Reactivarla la devuelve a `ACTIVO`.
- Nadie se da de baja a sí mismo y la plataforma nunca se queda sin una cuenta de administración activa.
- Cada cambio queda en la auditoría.

## Estado previo
`backend/admin-django` era solo un esqueleto con `curriculo/models.py` y `admin.py`, sin `manage.py`. No había forma de crear ni dar de baja cuentas sin SQL.

## Arquitectura
- **Django pasa a ser la API de administración** (`/api/admin/v1`, puerto 8082). Es el único escritor de `identidad_*` (regla de `AGENTS.md`).
- **No crea tablas.** No usa `auth`, `sessions`, `admin` ni `contenttypes` de Django. Los modelos son `managed = False` sobre las tablas existentes y las migraciones de Django están desactivadas.
- **Sesiones de la consola** en `identidad_sesion_usuario`:
  - `cliente = 'WEB_ADMIN'`;
  - cookie `rupi_admin` `HttpOnly`, `SameSite=Strict`, con ruta `/api/admin`;
  - duran 8 horas;
  - se guarda solo el SHA-256 del token.
- **Ingreso:** mismo límite de intentos que Spring (5 por cuenta y equipo, o 20 por equipo, en 5 minutos), registrado en `identidad_intento_acceso`.
- **Contraseñas:** BCrypt `$2b$12$`. `PasswordService` de Spring también las verifica, así que una cuenta creada en la consola entra a la web y al celular.
- **Escrituras:** exigen un `Origin` propio o de la web, con la misma regla que `SessionController.checkOrigin`.
- **Auditoría:** las acciones se registran en `operaciones_evento_auditoria` (`CUENTA_CREADA`, `CUENTA_MODIFICADA`, `CUENTA_DADA_DE_BAJA`, `CUENTA_REACTIVADA`).
- **Web:** la consola React vive en `/admin`. Vite reenvía `/api/admin` a Django.

## Datos · `V31__consola_administracion_cuentas_hu41.sql`
Rol `ADMINISTRADOR` y permisos `cuentas.ver` y `cuentas.gestionar`. `INSERT IGNORE` idempotente.

La primera cuenta de administración se crea con `python manage.py crear_administrador --usuario …`, que pide la contraseña sin mostrarla. Nunca va en una migración.

## Permisos MySQL · `grants-django-admin.sql`
Usuario `rupi_admin` con estos permisos:
- `SELECT` sobre todo;
- `INSERT` y `UPDATE` sobre `identidad_*` y `curriculo_*`;
- `INSERT` sobre `operaciones_evento_auditoria`.

## API
- `POST /sesion`, `GET /sesion` y `DELETE /sesion`: ingreso, sesión actual y salida.
- `GET /cuentas?q=&rol=&estado=&pagina=`: 20 por página.
- `POST /cuentas`: devuelve `claveTemporal` (por ejemplo, «condor-maiz-totora-48»).
- `GET /cuentas/{id}` y `PATCH /cuentas/{id}`.
- `POST /cuentas/{id}/baja` y `POST /cuentas/{id}/reactivar`.

## Uso
Abrir `http://localhost:5173/admin`. La consola tiene dos zonas:
- **Lista de cuentas:** buscador y filtros, con sellos de rol y estado.
- **Panel lateral:**
  - datos editables;
  - alta y baja con confirmación;
  - historial.

Al crear una cuenta aparece una boleta punteada con el usuario y la clave temporal, con un botón para copiarla.

## Pruebas
- `manage.py test`: 10 pruebas sin base de datos.
  - Claves compatibles con Spring (`$2a$`, `$2b$`, `pbkdf2_sha256`).
  - Usuario y correo.
  - `Origin`.
- `scripts/verify_admin_cuentas.py` contra Django, Spring y MySQL reales (16 comprobaciones de HU-41):
  - Spring acepta la clave temporal;
  - el nombre nuevo se ve en la web sin volver a ingresar;
  - la baja corta la sesión abierta;
  - una cuenta de baja no ingresa;
  - la cuenta reactivada vuelve a entrar;
  - la auditoría guarda los 4 eventos.
- Playwright a 375 y 1280 px: sin desbordes y todos los objetivos de 48 px o más.

## Pendiente
- Vincular la cuenta nueva a un aula. `escuela_membresia_aula` es de Spring; hoy el estudiante entra con el código de clase (HU-11).
- Cambio de clave obligatorio en el primer ingreso: no hay columna para marcarlo.
