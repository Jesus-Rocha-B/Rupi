# HU-45 · Salud de los servidores y tiempos de respuesta

Implementación completa: 29 de octubre de 2026.

> Como administrador del sistema, yo debería monitorear la salud de los servidores y el tiempo de respuesta de las operaciones a fin de anticipar cualquier fallo antes de que afecte las clases de los escolares.

## Subtareas completadas al 100%
1. **Configurar herramientas para medir el uso de CPU y RAM de los servidores:**
   - Spring Boot (`MonitorController.java`): cálculo de carga de CPU (`cpuPercent`, `processCpuPercent`) mediante `OperatingSystemMXBean` y memoria física del host (`usedMemoryMb`, `totalMemoryMb`, `memoryPercent`).
   - Django (`salud.py`): telemetría del host mediante `psutil` (CPU % y memoria virtual usada/total) expuesta en la consola administrativa.
2. **Desarrollar un servicio backend que registre el tiempo de respuesta de las operaciones:**
   - `RequestMetricsFilter.java` y `RequestMetrics.java`: percentiles p50 y p95, máximos, peticiones por minuto, tasa de errores 5xx y ranking de rutas lentas en ventana rodante de 15 minutos.
3. **Diseñar un panel web para visualizar las métricas de salud en tiempo real:**
   - React (`HealthView.tsx`): vista con fichas de estado en tiempo real para MySQL, API Spring Boot, Servicio de IA y Consola Django, tiempos de latencia, refresco automático cada 30 segundos y control de pausa.
4. **Programar alertas automáticas al detectar tiempos de respuesta altos o fallos inminentes:**
   - Motor preventivo en Django (`salud.py` / `evaluar_alertas`): evalúa caídas de servicios críticos (MySQL, API), latencias superiores a 800 ms, p95 superior a 1500 ms, tasas de errores 5xx superiores al 5 %, y saturación crítica de CPU (>= 85 %) o memoria RAM (>= 85 %).
   - Panel React (`HealthView.tsx`): sección destacada «Alertas automáticas del sistema» con niveles de severidad (`CRITICA`, `ADVERTENCIA`), mensaje explicativo, impacto pedagógico y recomendaciones correctivas.
