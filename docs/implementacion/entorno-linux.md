# Entorno local en Linux

Guía equivalente a la preparación de Windows. Describe cómo dejar Rupi funcionando en Linux sin `sudo` y sin tocar los servicios del sistema. Resumen de uso diario en [`LEEME-LOCAL-LINUX.md`](../../LEEME-LOCAL-LINUX.md).

## Compatibilidad

Probado únicamente en **Fedora 44** (x86_64). Los scripts no usan nada propio de Fedora, así que deberían servir en cualquier distribución con `bash`, glibc 2.28 o superior y arquitectura x86_64 (Debian, Ubuntu, Arch, openSUSE, etc.), pero no se han probado allí.

- **Arquitectura:** el MySQL portable es `x86_64`. En ARM hay que descargar el paquete `aarch64` y ajustar el nombre de carpeta.
- **No compatible:** distribuciones con musl (Alpine) y macOS.
- **Ejecución:** con `./iniciar-rupi.sh` o `bash iniciar-rupi.sh`; no con `sh`.
- **Requisitos previos:** `bash`, `curl`, Node 24 y Maven en el PATH. Se instalan con el gestor de paquetes de cada distribución (`dnf`, `apt`, `pacman`...) o con sus instaladores oficiales. Los scripts no los instalan.
- Los pasos de esta guía que mencionan Fedora (Java del sistema, MariaDB en el 3306) son ejemplos de la máquina probada; en otra distribución el servicio de base de datos del sistema y la versión de Java pueden diferir.

## Diferencias con Windows

| Tema | Windows | Linux |
|---|---|---|
| Inicio / parada | `Iniciar-Rupi.cmd`, `Iniciar-Rupi.ps1`, `Detener-Rupi.ps1` | `iniciar-rupi.sh`, `detener-rupi.sh` |
| Credenciales locales | `.local/env.ps1` | `.local/env.sh` |
| MySQL portable | `.local/mysql-8.4.11-winx64` (`mysqld.exe`) | `.local/mysql-8.4.11-linux-glibc2.28-x86_64` (`mysqld`) |
| Java 21 | Instalado en el sistema y en PATH | Temurin 21 en `~/jdks/`, fijado por `.local/env.sh` |
| Comandos Node/Maven | `npm.cmd`, `mvn.cmd` | `npm`, `mvn` |
| Verificación HU-01 | `scripts/Verificar-HU01.ps1` | Mismo script con `pwsh` (PowerShell 7) |
| Verificación HU-02/03 | `python scripts/verify_learning_flow.py` | `python3 -I scripts/verify_learning_flow.py` |

El código de la API, el frontend y el SQL son los mismos en ambos sistemas.

## Puertos y aislamiento

- 5173: web (Vite). 8081: API (Spring Boot). 3307: MySQL de Rupi, solo en 127.0.0.1.
- Fedora (y otras distribuciones) puede traer MariaDB o MySQL en el 3306. No se modifica ni se detiene. La instancia de Rupi usa su propia carpeta de datos y su propio socket (`.local/sockets/mysql.sock`).
- `iniciar-rupi.sh` reutiliza lo que ya escuche en 3307, 8081 y 5173. Si otro programa ocupa uno de esos puertos, el inicio falla; libera el puerto antes de reintentar.

## Preparación desde cero

Todo lo siguiente queda dentro de `~/jdks/` y de la carpeta ignorada `.local/`. Ningún paso requiere `sudo`.

### 1. Java 21

Fedora 44 no incluye `java-21-openjdk`; en otras distribuciones puedes usar su paquete de Java 21 si existe, siempre que `.local/env.sh` apunte a él. Descarga Temurin 21 de Adoptium (`https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse`), comprueba el SHA-256 publicado por la API de Adoptium y descomprime en `~/jdks/`. Comprueba con:

```bash
~/jdks/jdk-21.0.12.1+1/bin/java -version
```

### 2. MySQL 8.4

Descarga `mysql-8.4.11-linux-glibc2.28-x86_64.tar.xz` desde `https://cdn.mysql.com/Downloads/MySQL-8.4/`, verifica el MD5 publicado junto al archivo (`.md5`) y descomprímelo en `.local/`. Crea `.local/my.cnf` con:

```ini
[mysqld]
basedir=<ruta-del-repositorio>/.local/mysql-8.4.11-linux-glibc2.28-x86_64
datadir=<ruta-del-repositorio>/.local/mysql-data
port=3307
bind-address=127.0.0.1
mysqlx=OFF
socket=<ruta-del-repositorio>/.local/sockets/mysql.sock
pid-file=<ruta-del-repositorio>/.local/mysql.pid
log-error=<ruta-del-repositorio>/.local/mysql-error.log
character-set-server=utf8mb4
```

Inicializa y arranca la instancia:

```bash
mkdir -p .local/sockets .local/mysql-data
.local/mysql-8.4.11-linux-glibc2.28-x86_64/bin/mysqld --defaults-file=.local/my.cnf --initialize-insecure
./iniciar-rupi.sh   # o arranca mysqld a mano con el mismo --defaults-file
```

### 3. Usuarios y credenciales

Con la instancia encendida, define una contraseña para `root` y crea el usuario `rupi` desde el socket (`mysql -uroot -S .local/sockets/mysql.sock`). Genera ambas contraseñas con `openssl rand -hex 16` y guárdalas solo en estos archivos con permisos `600`:

- `.local/root.cnf`: sección `[client]` con `user=root`, `password`, `host=127.0.0.1` y `port=3307`.
- `.local/env.sh`: variables `RUPI_DB_URL` (`jdbc:mysql://127.0.0.1:3307/rupi?...`), `RUPI_DB_USERNAME`, `RUPI_DB_PASSWORD` y `JAVA_HOME`.

Permisos del usuario `rupi` (para `'rupi'@'127.0.0.1'` y `'rupi'@'localhost'`), el mínimo que usa la API:

- `SELECT` sobre `rupi.*`.
- `UPDATE` sobre `aprendizaje_progreso_nodo` y `aprendizaje_inscripcion_ruta`.
- `INSERT, UPDATE` sobre `identidad_sesion_usuario`.
- `INSERT` sobre `identidad_intento_acceso`.

Esto sigue la excepción de desarrollo descrita en [HU-02-HU-03.md](HU-02-HU-03.md): el login escribe tablas de identidad hasta reconciliarlo con Django.

### 4. Esquema y datos de prueba

Con `mysql --defaults-file=.local/root.cnf --default-character-set=utf8mb4 < archivo.sql`, en este orden:

1. `database/mysql/schema.sql`
2. `database/mysql/migrations/V04__curriculum_cultural_context.sql`
3. `database/mysql/seed-hu01.sql`
4. `database/mysql/migrations/V06__reparar_hash_demo.sql`
5. `database/mysql/seed-hu02.sql`
6. `database/mysql/migrations/V07__unidades_curriculares_hu04.sql` (unidades de prueba de HU-04; se puede repetir sin riesgo)

`V05__ultima_visita.sql` solo se aplica a bases anteriores: el `schema.sql` actual ya incluye esas columnas. Debe resultar un esquema `rupi` con 79 tablas y la cuenta `estudiante.demo` con hash `pbkdf2_sha256`. No repitas las semillas sobre progreso real.

### 5. API y web

```bash
npm install
./iniciar-rupi.sh --recompilar
```

`npm install` puede modificar `package-lock.json`; no incluyas ese cambio en commits propios del entorno.

## Evidencia de esta preparación

Compilación de la API con Java 21 y sus pruebas, `npm install` sin vulnerabilidades y `scripts/verify_learning_flow.py` con 23 comprobaciones HTTP/MySQL correctas contra MySQL 8.4.11 en Fedora. El script elimina solo los registros ficticios que crea.

## Puntos a vigilar entre Windows y Linux

- **Saltos de línea:** los archivos del repositorio usan LF. Si alguien en Windows activa `core.autocrlf`, pueden aparecer commits con CRLF; los `.sh` deben mantenerse con LF.
- **Mayúsculas y minúsculas:** Linux distingue `Foto.png` de `foto.png`; Windows no. Revisa imports y rutas de recursos con el mismo nombre exacto.
- **Codificación:** usa UTF-8 en editores, `utf8mb4` en MySQL y archivos UTF-8 con BOM para PowerShell 5.1.
- **Permisos:** los scripts `.sh` necesitan el bit de ejecución (`chmod +x`).
- **PowerShell:** AGENTS.md exige compatibilidad con PowerShell 5.1 y 7+. Los `.ps1` del repositorio no se modifican para Linux.
