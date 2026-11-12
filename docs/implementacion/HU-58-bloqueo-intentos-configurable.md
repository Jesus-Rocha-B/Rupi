# HU-58 · Bloqueo automático y límite de intentos configurables

Implementación: 11 de noviembre de 2026.

> Como administrador del sistema, yo quiero configurar políticas de bloqueo automático y limitación de intentos de inicio de sesión fallidos para prevenir ataques de fuerza bruta en las apps web y móvil.

## Criterios de aceptación (definidos en la implementación)
- La administración configura en la consola, sin tocar código:
  - el freno por equipo: fallos de una cuenta en el mismo equipo, fallos de un equipo con cualquier cuenta y la ventana de tiempo;
  - el bloqueo automático de la cuenta: si está activo, cuántos fallos lo provocan y cuánto dura.
- La política vale para la web y el celular (Spring) y también para el ingreso a la consola (Django).
- Mientras dura el bloqueo, ni la contraseña correcta entra. El mensaje dice cuántos minutos faltan y la respuesta trae `Retry-After`.
- Un usuario que no existe recibe la misma respuesta que uno bloqueado, así no se revela qué cuentas existen.
- La consola muestra las cuentas bloqueadas en ese momento y permite desbloquearlas. Al desbloquear o restablecer la contraseña dejan de contar los fallos anteriores.
- SOPORTE solo consulta (`acceso.ver`). Cambiar la política exige `acceso.configurar`, y desbloquear sigue exigiendo `credenciales.restablecer` (HU-44).
- Los valores iniciales mantienen los límites de antes: 5 fallos por cuenta y equipo y 20 por equipo, en 5 minutos. A eso se suma el bloqueo automático de 10 fallos, que dura 15 minutos.

## Por qué el bloqueo no se guarda en la cuenta
Spring atiende el ingreso de la web y el celular, pero `identidad_cuenta_usuario` es de Django (escritor único). Por eso el bloqueo se calcula al iniciar sesión a partir de `identidad_intento_acceso`, una tabla en la que Spring ya escribía:

1. Se leen los fallos de contraseña (`INCORRECTO`) del nombre de usuario de los últimos *ventana + duración* minutos. Solo cuentan los posteriores a `intentos_reiniciados_en`.
2. Cada vez que *N* fallos caben en la ventana empieza un bloqueo que dura *duración* minutos desde el último de ellos.
3. Mientras dura el bloqueo, la contraseña no se prueba. El intento queda como `BLOQUEADO`, así que no alarga el bloqueo, y los fallos que ya lo causaron no vuelven a contar.

El cálculo es el mismo en `LoginPolicy.java` y en `identidad/politica_acceso.py`, y los dos tienen sus pruebas.

## Datos · `V46__politica_bloqueo_acceso_hu58.sql`
- `identidad_politica_acceso`: una sola fila (`id = 1`). Los `CHECK` limitan los rangos y exigen que el límite del equipo no sea menor que el de una cuenta en ese equipo, porque en el aula varias niñas y niños comparten la computadora.
- `identidad_cuenta_usuario.intentos_reiniciados_en`: la escribe Django al desbloquear o restablecer la contraseña.
- Índice `ix_intento_resultado_fecha` para listar las cuentas bloqueadas.
- Permisos `acceso.ver` (ADMINISTRADOR y SOPORTE) y `acceso.configurar` (ADMINISTRADOR).

Después de V46 hay que volver a ejecutar `grants-django-admin.sql`, que da INSERT y UPDATE en la tabla nueva. Spring solo necesita SELECT, que ya tiene. Si la tabla no existe, Spring usa los valores de siempre.

## API
**Django** (`/api/admin/v1`, sesión administrativa y Origin permitido en escrituras):

| Método y ruta | Uso |
|---|---|
| `GET acceso/politica` | Política, rangos permitidos, último cambio, cuentas bloqueadas ahora y resumen de 24 h. |
| `PUT acceso/politica` | Guarda la política completa y la registra en la auditoría (`POLITICA_ACCESO_ACTUALIZADA`). |
| `POST cuentas/{id}/desbloquear` | Ya existía (HU-44). Ahora también levanta el bloqueo automático y reinicia los intentos. |

El detalle y la lista de cuentas incluyen `bloqueoAutomatico`, `bloqueadoHasta` muestra el fin del bloqueo y el filtro «Bloqueadas» también las encuentra.

**Spring** (`POST /api/v1/auth/login`):
- `429`: freno por equipo, con «Espera N minutos» y `Retry-After`.
- `423` con minutos: bloqueo automático, con `Retry-After`.
- `423` sin minutos: bloqueo pedido a la consola (HU-44), sin cambios.

## Pantallas
- **Consola → Usuarios → Bloqueo por intentos**:
  - resumen de las últimas 24 horas;
  - los dos grupos de ajustes, cada uno con su rango;
  - una frase «Así funcionará» que se actualiza mientras se edita;
  - el botón «Valores recomendados»;
  - la lista «Cuentas bloqueadas ahora» con el botón Desbloquear.
- **Panel de la cuenta**: explica que la cuenta se bloqueó sola y hasta cuándo.
- **Ingreso del estudiante**: «Por seguridad, tu cuenta se bloqueó un ratito. Vuelve a probar en N minutos o pide ayuda a tu profe.»
- **Celular**: no cambia nada, porque ya muestra el mensaje que envía la API.

## Límites conocidos
- La IP que ve Spring es la de la conexión. Detrás de un proxy, todo el colegio compartiría el freno por equipo hasta que se configure la IP real del cliente.
- Alguien que conozca un nombre de usuario puede bloquear esa cuenta a propósito. El bloqueo termina solo y la consola lo levanta al instante. La consola tampoco deja que alguien se desbloquee a sí mismo, así que una cuenta de administración bloqueada necesita a otra persona de administración o esperar los minutos que falten.

## Verificación
- `LoginPolicyTest` (11 pruebas, Spring) y `identidad/test_politica_acceso.py` (14 pruebas, Django) prueban:
  - el cálculo del bloqueo y su fin;
  - que los fallos ya usados no vuelvan a contar;
  - los minutos restantes;
  - la validación de rangos y tipos;
  - los mensajes 423 y 429 con `Retry-After`.
- `scripts/verify_hu58_bloqueo_intentos.py` hace 29 comprobaciones contra Django, Spring y MySQL reales, con dos cuentas temporales. Deja la base como estaba.
- Playwright a 1280 y 375 px: consola, panel de la cuenta e ingreso del estudiante, sin desborde y con controles de 48 px.
