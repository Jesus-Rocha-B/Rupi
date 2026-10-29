# HU-45 · Salud de los servidores y tiempos de respuesta

Implementación: 29 de octubre de 2026 · commit `edeb965`.

> Como administrador del sistema, quiero monitorear la salud de los servidores y el tiempo de respuesta de las operaciones, para anticipar cualquier falla antes de que afecte las clases.

## Criterios de aceptación (definidos en la implementación)
- Una pestaña «Salud» muestra, en una sola vista, si responden la base de datos, la API de Rupi, el servicio de IA y la consola, y cuánto tarda cada uno.
- Muestra los tiempos de respuesta de la API de los últimos 15 minutos: peticiones, por minuto, mediana, p95, máximo y errores 5xx. También las rutas más lentas.
- **Titular con el estado general:**
  - «Todo en orden»;
  - «Conviene revisar» si algo está lento o falta la IA;
  - «Hay una falla que afecta a las clases» si cae la base o la API.
  En todos los casos se nombra lo que falla. El estado se indica con texto e ícono, no solo con color.
- Se revisa sola cada 30 s mientras la pestaña está visible. Se puede pausar o pedir una revisión al momento.
- Solo la administración la ve (permiso `plataforma.monitorear`). Los tiempos internos de Spring solo se entregan con un token.

## Estado previo
No había monitoreo. El servicio de IA tenía `/health`, pero nadie lo consultaba.

## Spring · `pe.rupi.api.monitor`
- **`RequestMetricsFilter`:**
  - mide cada petición a `/api` completa, incluida la autenticación;
  - la agrupa por patrón de ruta (`GET /api/v1/student/learning-routes/{id}`), así no se crea una entrada por cada id;
  - las rechazadas antes de llegar a un controlador quedan como «(sin sesión o ruta inexistente)»;
  - el propio monitor no se mide.
- **`RequestMetrics`:** guarda en memoria las últimas 4096 peticiones y 256 por ruta, con un máximo de 300 rutas. Calcula percentiles por rango más cercano y la ventana de 15 minutos. No hay tablas nuevas: se reinicia con la API.
- **`MonitorController`:** `GET /api/v1/monitor/health` devuelve el estado, el ping a MySQL, la memoria y los hilos de la JVM y los tiempos.
  - Exige `X-Rupi-Monitor-Token` igual a `rupi.monitor.token`, comparado en tiempo constante.
  - Sin token configurado responde 404; con un token equivocado, 401.

## Django · `operaciones/salud.py`
- Revisa MySQL (latencia, versión y conexiones), la API (con el token) y la IA (`/health`, solo datos no secretos). La API y la IA se consultan en paralelo, con 3 s de espera máxima.
- **Umbrales:** lento si una revisión tarda 800 ms o más, si el p95 llega a 1500 ms o si falla el 5 % de las peticiones (errores 5xx).
- **Estado general:** `FALLA` si cae la base o la API; `ATENCION` si algo está lento, sin configurar o falta la IA.
- `RUPI_MONITOR_TOKEN` se comparte entre la API y la consola. Los scripts de inicio lo generan una vez en `.local/monitor.token`.

## Datos · `V35__salud_plataforma_hu45.sql`
Permiso `plataforma.monitorear` solo para `ADMINISTRADOR`. No crea tablas.

## Pruebas
- `RequestMetricsTest` (5):
  - percentiles;
  - las rutas lentas van primero y se cuentan los errores 5xx;
  - las peticiones viejas salen de la ventana de 15 minutos;
  - 404 sin token y 401 con un token equivocado;
  - si la base cae, el estado es `DEGRADED`.
- `operaciones/tests.py` (4): umbrales y estado general.
- `tests/healthText.test.ts` (4): unidades, tiempo encendida, titular y ficha de la API.
- `verify_admin_cuentas.py` (+9 comprobaciones de HU-45):
  - 401 sin token o con uno equivocado;
  - revisa los cuatro servicios;
  - agrupa por ruta y no cuenta el monitor;
  - el estado general es coherente;
  - la mesa de ayuda recibe 403.
- Playwright a 375 y 1280 px:
  - con la IA apagada: «Conviene revisar»;
  - con Spring detenido a propósito: «Hay una falla que afecta a las clases».

## Pendiente
- Alertas tempranas cuando algo se cae (HU-47) y tablero con disponibilidad histórica (HU-48): hoy solo hay métricas en el momento, sin historial.
