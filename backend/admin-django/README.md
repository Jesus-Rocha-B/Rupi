# Django · Administración

API JSON de la consola de administración (`/admin` en la web). Administra identidad (cuentas, roles, permisos y sesiones de administración) y, más adelante, el catálogo curricular y la configuración global de IA.

## Reglas de integración

- Se conecta al esquema MySQL `rupi` definido en `../../database/mysql/`.
- Es el único escritor de `identidad_*` y `curriculo_*`; no modifica las tablas cuyo escritor es Spring Boot. Registra sus propias acciones en `operaciones_evento_auditoria`.
- **No crea tablas.** No usa `django.contrib.auth`, `sessions`, `admin` ni `contenttypes`. Los modelos son `managed = False` y `MIGRATION_MODULES` desactiva las migraciones de Django: el esquema se versiona en `database/mysql/migrations/`.
- Las sesiones de la consola viven en `identidad_sesion_usuario` con `cliente = 'WEB_ADMIN'` (cookie `rupi_admin`, `HttpOnly`, `SameSite=Strict`, ruta `/api/admin`, 8 horas). Solo se guarda el SHA-256 del token.
- Las contraseñas se guardan con BCrypt (`$2b$12$`), que `PasswordService` de Spring también verifica.
- Toda escritura exige un `Origin` propio o de la web (igual que `SessionController.checkOrigin`).
- No guardar secretos en el repositorio. Usar variables de entorno en desarrollo y en el proveedor de despliegue.

`curriculo/` conserva los modelos del catálogo, todavía fuera de `INSTALLED_APPS` hasta que una historia lo necesite.

## Puesta en marcha local

```bash
cd backend/admin-django
python3 -m venv .venv                       # Windows: py -m venv .venv
.venv/bin/pip install -r requirements.txt   # Windows: .venv\Scripts\pip install -r requirements.txt
```

1. Crear el usuario MySQL `rupi_admin` y darle permisos (como root):
   `mysql -e "SET @admin_usuario='rupi_admin'; SOURCE database/mysql/grants-django-admin.sql"`
2. Aplicar `database/mysql/migrations/V31__consola_administracion_cuentas_hu41.sql`, `V32__roles_permisos_estrictos_hu42.sql` y las siguientes.
3. Variables en `.local/env.sh` (o `.local/env.ps1`): `RUPI_ADMIN_DB_PORT`, `RUPI_ADMIN_DB_USERNAME`, `RUPI_ADMIN_DB_PASSWORD`.
4. Crear la primera cuenta de administración (pide la contraseña sin mostrarla):
   `.venv/bin/python manage.py crear_administrador --usuario admin.rupi --nombre Rosa --apellido Quispe`
5. `./iniciar-rupi.sh` levanta la API en `127.0.0.1:8082`; Vite reenvía `/api/admin` a ese puerto. Abrir `http://localhost:5173/admin`.

`runserver` es solo para desarrollo. En despliegue se sirve `rupi_admin.wsgi` con un servidor WSGI y se fijan `RUPI_ADMIN_SECRET_KEY`, `RUPI_ADMIN_ALLOWED_HOSTS` y `RUPI_ADMIN_ORIGINS`.

## Variables

| Variable | Por defecto | Uso |
|---|---|---|
| `RUPI_ADMIN_DB_HOST` / `_PORT` / `_NAME` | `127.0.0.1` / `3306` / `rupi` | Conexión MySQL |
| `RUPI_ADMIN_DB_USERNAME` / `_PASSWORD` | `rupi_admin` / vacío | Credenciales MySQL |
| `RUPI_ADMIN_ALLOWED_HOSTS` | `127.0.0.1,localhost` | Hosts aceptados |
| `RUPI_ADMIN_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Orígenes que pueden escribir |
| `RUPI_ADMIN_SECRET_KEY` | aleatoria por proceso | Fijarla en despliegue |
| `RUPI_ADMIN_DEBUG` | apagado | Solo en desarrollo |

## API (`/api/admin/v1`)

| Método y ruta | Permiso | Historia |
|---|---|---|
| `GET/POST/DELETE sesion` | — | Ingreso y salida de la consola |
| `GET cuentas?q=&rol=&estado=&pagina=` | `cuentas.ver` | HU-41 |
| `POST cuentas` | `cuentas.gestionar` | HU-41 · devuelve `claveTemporal` una sola vez |
| `GET/PATCH cuentas/{id}` | `cuentas.ver` / `cuentas.gestionar` | HU-41 |
| `POST cuentas/{id}/baja` · `POST cuentas/{id}/reactivar` | `cuentas.gestionar` | HU-41 |
| `GET roles` | `cuentas.ver` | HU-42 · roles, permisos y cuántas cuentas tiene cada uno |
| `PUT cuentas/{id}/roles` `{"roles": [...]}` | `roles.asignar` | HU-42 |

### Roles y permisos (HU-42)

- Entra a la consola quien tenga `consola.ingresar` (ADMINISTRADOR y SOPORTE), no un rol fijo. Los permisos se leen en cada petición: retirar un rol corta el acceso de inmediato.
- Los permisos de cada rol se fijan en las migraciones (V31, V32…); la consola solo asigna roles.
- Sin `cuentas.datos_personales` el correo llega enmascarado (`l•••@rupi.pe`) y no se puede buscar por correo.
- Reglas: toda cuenta tiene al menos un rol; ESTUDIANTE no se combina con otros; nadie se quita su propio rol de administración; siempre queda una administración activa. Cambiar roles cierra la sesión abierta de esa cuenta.
- Spring rechaza con 403 en la web y el celular las cuentas que solo tienen roles de consola.

Los errores responden `{"detail": "..."}` en español.

## Pruebas

```bash
.venv/bin/python manage.py test          # sin base de datos
python3 -I scripts/verify_admin_cuentas.py   # contra Django y Spring levantados (desde la raíz)
```
