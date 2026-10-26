# Guía de Conexión · Base de Datos Compartida en la Nube (TiDB Cloud)

Para que todo el equipo de desarrollo trabaje sincronizado sobre los mismos datos, usuarios, rutas de aprendizaje y actividades escolares, el proyecto cuenta con un cluster gestionado en **TiDB Cloud Serverless** (compatible al 100% con el protocolo MySQL 8).

> **Seguridad:** Por políticas de seguridad del proyecto, las credenciales y contraseñas de acceso **nunca se guardan en el repositorio Git**. Cada desarrollador las configura únicamente en su archivo local `.local/env.ps1` (el cual está protegido por `.gitignore`).

---

## 1. Configuración Local del Desarrollador

Cada miembro del equipo debe crear o editar su archivo local en la raíz del proyecto:
📂 `.local/env.ps1`

Copia y pega la siguiente configuración (solicita la contraseña al administrador del equipo por el canal privado):

```powershell
# Configuración de Base de Datos Compartida (TiDB Cloud Serverless)
$env:RUPI_DB_URL = 'jdbc:mysql://gateway01.sa-east-1.prod.aws.tidbcloud.com:4000/rupi?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&sslMode=VERIFY_IDENTITY'
$env:RUPI_DB_USERNAME = '36yz8QtW6CP6gXb.root'
$env:RUPI_DB_PASSWORD = '<SOLICITAR_CONTRASENA_AL_EQUIPO>'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:PORT = '8081'
$env:RUPI_DEMO_SESSION_TOKEN = 'df074881786d423893c8137b54eca3a00e28f2eaa6ee41ed902f8743766b1744'
```

---

## 2. Iniciar el Backend

Una vez configurado `.local/env.ps1`, inicia el servidor de Spring Boot ejecutando en PowerShell:

```powershell
powershell -File scripts/Iniciar-Backend-Daemon.ps1
```

El backend se iniciará en el puerto `8081` conectado a la nube en tiempo real.

---

## 3. Cómo Probar la Conexión

Para verificar que tu backend está correctamente autenticado contra la base de datos compartida, ejecuta:

```powershell
powershell -File scripts/Probar-Login.ps1
```

Si la conexión es exitosa, verás la respuesta con los datos de:
- **Estudiante Demo:** Mateo (3.° de primaria, I.E. 38001 Mariscal Sucre)
- **Docente Demo:** Prof. Carlos (Docente de aula)

---

## 4. Iniciar el Frontend

Para abrir la interfaz web:

```powershell
npm --prefix frontend/web-react run dev
```

Abre tu navegador en `http://localhost:5173`. Todos los cambios que hagas o avances que completes se reflejarán instantáneamente para todos los miembros del equipo.
