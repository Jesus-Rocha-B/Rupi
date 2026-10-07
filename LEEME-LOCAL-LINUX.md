# RUPI en Linux

Equivale a `LEEME-LOCAL.md`, que describe Windows. Probado en Fedora 44 (x86_64); debería servir en otras distribuciones con `bash` y glibc 2.28 o superior, pero no se ha probado allí. No funciona en Alpine ni en macOS, y en ARM requiere el MySQL `aarch64` (ver `docs/implementacion/entorno-linux.md`). En el entorno preparado ejecuta desde la carpeta del repositorio:

```bash
./iniciar-rupi.sh
```

Web: http://localhost:5173. API local: 8081. MySQL portable: 3307.

Para detener la web, la API y la base local: `./detener-rupi.sh`.

Cuenta ficticia de desarrollo: `estudiante.demo`, contraseña `123456`. Solo tiene asignada Matemática de segundo de primaria. Otros cursos se muestran como no disponibles hasta que exista contenido publicado y una matrícula válida. La cuenta no es para despliegue público.

El mapa conserva sus estados iniciales: dos actividades completadas, una en curso, una disponible y las demás bloqueadas. HU-02 permite abrir contenido habilitado; HU-03 recuerda la última parada y ruta en MySQL.

## Requisitos y datos privados

Node 24 y Maven deben estar instalados y disponibles en PATH. Java 21 no se toma del sistema: `.local/env.sh` fija `JAVA_HOME` en `~/jdks/jdk-21.0.12.1+1` (Temurin 21). Fedora 44 ya no publica `java-21-openjdk`, y el Java del sistema puede ser otro (por ejemplo 25) sin que afecte a Rupi.

`.local/env.sh`, `.local/root.cnf`, `.local/my.cnf` y `.local/mysql-data` pertenecen a este dispositivo: no se suben a GitHub ni se copian como parte del código. No borres `mysql-data`: conserva el progreso.

Una clonación nueva requiere preparar MySQL y credenciales propias; los scripts de inicio no instalan dependencias. Ver `docs/implementacion/entorno-linux.md` para la preparación paso a paso. No cargues de nuevo la semilla inicial sobre el avance real de estudiantes.

Para compilar Java después de modificarlo, ejecuta `./iniciar-rupi.sh --recompilar` con la API detenida. Vite actualiza automáticamente los cambios de React. `detener-rupi.sh` detiene los procesos registrados por el script y la base local; una terminal de Vite abierta manualmente se detiene con Ctrl+C.

## Convivencia con MariaDB del sistema

Fedora instala MariaDB, que escucha en el puerto 3306 (otras distribuciones pueden tener MariaDB o MySQL ahí). Rupi usa su propio MySQL 8.4 en el 3307, con datos y socket dentro de `.local/`. Ambos pueden estar encendidos a la vez. Los scripts de Rupi nunca usan, detienen ni configuran el servicio del sistema. El comando `mysql` del sistema es el de MariaDB: para consultar la base de Rupi usa el cliente de `.local/mysql-8.4.*/bin/mysql` con `--defaults-file=.local/root.cnf`.

## Verificaciones

```bash
JAVA_HOME=~/jdks/jdk-21.0.12.1+1 mvn -f backend/api-spring-boot/pom.xml test
npm run build
npm run lint
node --experimental-strip-types --test frontend/web-react/tests/routeSelection.test.ts
python3 -I scripts/verify_learning_flow.py --root-config .local/root.cnf --mysql .local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysql
```

Los scripts `scripts/Verificar-HU01.ps1` y `scripts/Verificar-Auth.ps1` requieren PowerShell 7 (`pwsh`). `Iniciar-Rupi.ps1` y `Iniciar-Rupi.cmd` son solo para Windows.

La finalización de actividades, calificación, puntos y desbloqueo de niveles futuros no forman parte de HU-02/HU-03. Django y Android se implementan en sus propias historias.
