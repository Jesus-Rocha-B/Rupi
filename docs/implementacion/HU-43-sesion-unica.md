# HU-43 · Sesión única sincronizada entre la web y el celular

Implementación: 29 de octubre de 2026 · commit `838b0d7`.

> Como administrador del sistema, quiero habilitar un mecanismo de sesión única sincronizada entre la web y el móvil, para evitar accesos simultáneos conflictivos con una sola cuenta.

## Criterios de aceptación (definidos en la implementación)
- Una cuenta tiene como mucho una sesión activa entre la web, el celular y la consola.
- Al ingresar en un lugar se cierra la sesión abierta en cualquier otro.
- La pantalla cuya sesión se cerró explica por qué. Por ejemplo: «Tu cuenta se abrió en un celular…». No muestra solo el ingreso.
- La web se entera aunque esté quieta: comprueba la sesión cada minuto y al volver a la pestaña.
- **Desde la consola, la administración y la mesa de ayuda pueden:**
  - ver dónde está abierta cada cuenta (web, celular o consola) y su última actividad;
  - cerrar una sesión, por ejemplo si se perdió una tableta del aula.
- En Android, si la sesión se cierra con la app en segundo plano, los avisos se pausan y llega una notificación que lo explica.

## Estado previo
- El índice `UNIQUE` sobre `usuario_activo_id` (columna generada) ya impedía dos sesiones activas.
- Spring ya revocaba las anteriores al ingresar.
- Pero todas las sesiones quedaban como `WEB_USUARIO`, nadie sabía por qué se cerraba una sesión y `actividad_en` nunca cambiaba.

## Spring
- **Cliente de la sesión:** el celular envía `X-Rupi-Cliente: MOVIL` y su sesión se guarda como `MOVIL`. Cualquier otro valor cuenta como `WEB_USUARIO`. `find()` acepta las dos.
- **Motivo del 401 (`sessionEnd`):** el 401 trae `X-Rupi-Sesion-Cerrada` con uno de estos valores.
  - `OTRO_DISPOSITIVO`: la sesión se revocó y en ese mismo momento (entre −1 y +5 s) se abrió otra más nueva de la misma cuenta. También trae `X-Rupi-Sesion-Nueva` con `WEB_USUARIO`, `MOVIL` o `WEB_ADMIN`.
  - `CERRADA`: salida, baja o cierre desde la consola.
  - `EXPIRADA`.
- **Dónde se aplica:** en `StudentAuthenticationFilter` y en `GET /api/v1/auth/session`. CORS expone las dos cabeceras.
- **Última actividad:** `actividad_en` se actualiza como mucho cada 5 minutos por sesión (memoria local), sin una escritura por petición.

## Web
- `ApiError` trae `sessionEnd` y `sessionNewClient`.
- `utils/sessionEnd.ts` arma el mensaje.
- `App.tsx` llama a `auth/session` cada 60 s y en `visibilitychange`.
- `LoginView` muestra el aviso con el ícono de celular.

## Android
- `RupiApi` envía `X-Rupi-Cliente: MOVIL` y convierte el 401 en `RupiSessionExpired` con el motivo.
- `MainActivity` muestra ese texto en el ingreso.
- `StudyAlertJob` pausa los avisos y publica «Tus avisos de Rupi están en pausa» cuando la cuenta se abrió en otro lugar.

## Consola · `V33__sesion_unica_web_movil_hu43.sql`
- **Permiso `sesiones.gestionar`:** lo tienen administración y mesa de ayuda.
- **Pestaña «Sesiones»:**
  - sello «Una sola sesión por cuenta» (activa para todas y garantizada por la base);
  - totales por lugar y filtros;
  - lista con «Cerrar sesión».
- **Panel de cada cuenta:** bloque «Sesión».
- **API:**
  - `GET /sesiones?cliente=`;
  - `POST /cuentas/{id}/cerrar-sesion`, que audita `SESION_CERRADA` y responde 409 para la propia cuenta («usa Salir»).
- **La consola misma:** si la cuenta de administración entra en otro equipo, la primera consola recibe un 401 que lo explica (`motivo_cierre`).

## Pruebas
- `SingleSessionTest` (4):
  - el cliente se identifica y no se acepta `WEB_ADMIN` forzado;
  - el 401 trae el motivo;
  - un token desconocido recibe un 401 simple;
  - el login pasa el cliente a la sesión.
- `tests/sessionEnd.test.ts` (4): mensajes según el motivo y el lugar.
- `verify_admin_cuentas.py` (+15 comprobaciones de HU-43):
  - con la misma cuenta en la web y en el celular, la web recibe un 401 `OTRO_DISPOSITIVO`/`MOVIL`;
  - en MySQL queda una sola sesión activa;
  - la consola la ve en el celular y la cierra;
  - el celular recibe `CERRADA`;
  - una segunda consola cierra la primera con el motivo.
- `assembleDebug` correcto.
