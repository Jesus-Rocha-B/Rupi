# RUPI en Windows

En el entorno preparado abre `Iniciar-Rupi.cmd` desde la carpeta del repositorio. También puedes ejecutar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Rupi.ps1
```

Web: http://localhost:5173. API local: 8081. MySQL portable: 3307.

Cuenta ficticia de desarrollo: `estudiante.demo`, contraseña `123456`. Solo tiene asignada Matemática de segundo de primaria. Otros cursos se muestran como no disponibles hasta que exista contenido publicado y una matrícula válida. La cuenta no es para despliegue público.

El mapa conserva sus estados iniciales: dos actividades completadas, una en curso, una disponible y las demás bloqueadas. HU-02 permite abrir contenido habilitado; HU-03 recuerda la última parada y ruta en MySQL.

## Requisitos y datos privados

Node 24, Java 21 y Maven deben estar instalados y disponibles en PATH. El inicio no fija la carpeta de instalación de Java ni de Maven. `.local/env.ps1`, `.local/root.cnf` y `.local/mysql-data` pertenecen a este dispositivo: no se suben a GitHub ni se copian como parte del código. No borres mysql-data: conserva el progreso.

Una clonación nueva requiere preparar MySQL y credenciales propias; el script de inicio no instala dependencias. Ver `docs/implementacion/HU-02-HU-03.md` para el orden de esquema, migraciones y semillas. No cargues de nuevo la semilla inicial sobre el avance real de estudiantes.

Para compilar Java después de modificarlo, detén primero la API y ejecuta el script con `-Recompilar`. Vite actualiza automáticamente los cambios de React. `Detener-Rupi.ps1` detiene los procesos registrados por el script y la base local; una terminal de Vite abierta manualmente se detiene con Ctrl+C.

## Verificaciones

```powershell
npm.cmd run build
npm.cmd run lint
node --experimental-strip-types --test frontend/web-react/tests/routeSelection.test.ts
mvn.cmd -f backend/api-spring-boot/pom.xml test
```

La finalización de actividades, calificación, puntos y desbloqueo de niveles futuros no forman parte de HU-02/HU-03. Django y Android se implementan en sus propias historias.
