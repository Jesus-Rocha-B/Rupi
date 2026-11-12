# HU-60 · Autenticación de doble factor (2FA) obligatoria

Implementación: 12 de noviembre de 2026.

> Como administrador del sistema, yo quiero implementar autenticación de doble factor (2FA) obligatoria para todas las cuentas con privilegios de administración o roles docentes.

## Criterios de aceptación implementados
- **Base de datos para guardar la clave secreta del doble factor (`V48__doble_factor_obligatorio_hu60.sql`):**
  - Nueva tabla `identidad_doble_factor` (`usuario_id` PK/FK hacia `identidad_cuenta_usuario`, `secreto_base32`, `verificado`, `ultimo_paso_usado`, `activado_en`, `actualizado_en`).
  - Permiso `seguridad.2fa` asignado al rol `ADMINISTRADOR`.
  - Cumple el principio de escritor único (`AGENTS.md`): Django (`rupi_admin`) escribe en `identidad_doble_factor` y Spring Boot (`rupi_api`) únicamente realiza lecturas `SELECT` al validar inicios de sesión en el portal web y móvil.
- **Servicio backend para generar y validar códigos temporales (RFC 6238 TOTP):**
  - Django (`backend/admin-django/identidad/doble_factor.py`): generación criptográfica de secretos de 160 bits en Base32 (`secrets.token_bytes`), construcción de URIs `otpauth://totp/Rupi:<usuario>?secret=...&issuer=Rupi&algorithm=SHA1&digits=6&period=30`, cálculo/verificación TOTP SHA-1 en ventana `±1` (`30 s`), protección antirrepetición (`ultimo_paso_usado`) y comparación en tiempo constante (`hmac.compare_digest`).
  - Spring Boot (`backend/api-spring-boot/src/main/java/pe/rupi/api/shared/TotpService.java`): verificación TOTP RFC 6238 en Java para `POST /api/v1/auth/login` con comparación `MessageDigest.isEqual` y control en memoria del último paso utilizado por cuenta.
- **Restricción obligatoria en el inicio de sesión (`ADMINISTRADOR`, `SOPORTE`, `DOCENTE`):**
  - **Consola admin Django (`identidad/sesion.py` y `/api/admin/v1/sesion`):** tras validar contraseña, exige el segundo factor antes de abrir sesión `WEB_ADMIN`. Si la cuenta aún no verificó su secreto TOTP responde `202` con `requiere2fa: true, configurado: false, otpauthUri, secretoBase32, desafio`; si ya lo configuró responde `202` con `configurado: true, desafio`. La verificación se completa en `POST /api/admin/v1/sesion/2fa` (o enviando `codigo` en `POST /api/admin/v1/sesion`).
  - **Portal docente / estudiante Spring Boot (`StudentSessionService.java` y `SessionController.java`):** `ESTUDIANTE` mantiene el acceso directo con usuario y contraseña; `ADMINISTRADOR`, `SOPORTE` y `DOCENTE` requieren `totpCode` válido: si aún no activaron 2FA responde `428` (`TOTP_SETUP_REQUIRED`), si falta el código responde `428` (`TOTP_REQUIRED`) y si el token es incorrecto responde `401` (`TOTP_INVALID`).
- **Interfaz web para configurar y escanear el código QR:**
  - Generador QR puro en TypeScript (`frontend/web-react/src/utils/qrCode.ts`) + componente SVG accesible (`src/components/QrCodeSvg.tsx`) + tarjeta reutilizable de enrolamiento (`src/components/TwoFactorSetupCard.tsx`).
  - **Consola web (`AdminLogin.tsx` y `TwoFactorView.tsx`):** enrolamiento obligatorio al primer ingreso y sección **Usuarios → Doble factor (2FA)** para rotar el QR propio, supervisar el estado 2FA de cuentas administrativas/docentes y reiniciar el QR de una cuenta cuando cambia de celular.
  - **Portal docente (`LoginView.tsx`):** al ingresar como docente sin 2FA activo inicia el enrolamiento guiado en Django (`/api/admin/v1/2fa/docente/iniciar` y `/api/admin/v1/2fa/docente/confirmar`), muestra el código QR para escanear con Google Authenticator / Aegis / Authy y permite completar el ingreso en Spring Boot inmediatamente después.

## Endpoints agregados
- **Django (`/api/admin/v1/`):**
  - `POST sesion` (actualizado: `202` cuando falta 2FA o retorna sesión si ya se validó el código)
  - `POST sesion/2fa`
  - `POST 2fa/docente/iniciar`
  - `POST 2fa/docente/confirmar`
  - `GET seguridad/2fa`
  - `POST seguridad/2fa/mi-rotacion`
  - `POST seguridad/2fa/mi-confirmacion`
  - `POST seguridad/2fa/cuentas/<uuid>/reiniciar` (`seguridad.2fa`)
- **Spring Boot (`/api/v1/auth/login`):**
  - Campo opcional `totpCode` en `LoginRequest`; códigos de error estructurados `TOTP_SETUP_REQUIRED` (`428`), `TOTP_REQUIRED` (`428`) y `TOTP_INVALID` (`401`).

## Verificación
- Pruebas Django: `python backend/admin-django/manage.py test identidad`
- Pruebas Spring Boot: `mvn.cmd test -Dtest=TotpServiceTest,ConsoleOnlyAccountTest,LockedAccountTest,LoginPolicyTest,SingleSessionTest,PasswordServiceTest,StudentAuthenticationFilterTest`
- Pruebas frontend y compilación: `npm test`, `npm run build` y `npx eslint` en `frontend/web-react`
- Verificación end-to-end contra servicios locales vivos: `scripts/verify_hu60_doble_factor.py`
