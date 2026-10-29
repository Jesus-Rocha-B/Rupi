# Servicio de IA (FastAPI)

Genera rutas de aprendizaje a partir de archivos o texto del docente y atiende el tutor Rupi. Habla solo con Gemini y escucha en `127.0.0.1:8000`; la web llega por el proxy de Vite (`/api/v1/ia`).

```bash
cd backend/service-ia
python3 -m venv .venv
.venv/bin/pip install -r requirements-dev.txt
GEMINI_API_KEY=... .venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 8000
.venv/bin/python -m unittest test_servicio_ia   # sin red ni clave
```

- **Clave:** `GEMINI_API_KEY` (o `RUPI_AI_KEY`) se envía en la cabecera `x-goog-api-key`, nunca en la URL.
- **Errores:** sin clave responde 503; si Gemini falla, 502; si un archivo no se puede leer, 400. El detalle queda solo en el log del servidor.
- **CORS:** solo los orígenes de `RUPI_IA_ORIGINS` (por defecto, la web en el puerto 5173) y sin credenciales.
- **Límites de entrada:**
  - hasta 5 archivos de 10 MB y texto de 60 000 caracteres;
  - en el tutor, mensaje de 2000 caracteres;
  - el contexto y el historial largos se recortan (se usan los últimos 10 mensajes).
- **Validación:** la ruta que devuelve Gemini debe tener título y de 1 a 12 paradas. Un tipo de actividad desconocido pasa a `LECCION` y una pregunta incoherente se descarta.

**Pendiente:**
- El servicio no exige sesión: cualquiera que llegue al proxy puede usar la cuota de Gemini.
- Unificar el proveedor con `ChatQuizProvider` de Spring (`RUPI_AI_ENDPOINT`), y con ello decidir si `Iniciar-IA-Daemon.ps1` debe cargar la clave guardada con DPAPI.
