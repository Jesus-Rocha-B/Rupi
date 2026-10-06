# Ejecutar Rupi en esta computadora

Abre `C:\Rupi\Iniciar-Rupi.cmd` y visita http://localhost:5173.

Desde PowerShell tambien puedes ejecutar:

```powershell
cd C:\Rupi
powershell -NoProfile -ExecutionPolicy Bypass -File .\Iniciar-Rupi.ps1
```

Para detener los servicios:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\Rupi\Detener-Rupi.ps1
```

React se ejecuta en el puerto 5173, Spring Boot en 8081 y MySQL 8.4.11 en 3307. Los servicios solo escuchan conexiones locales. La base propia del proyecto contiene las 77 tablas del esquema y la semilla HU-01: un estudiante ficticio y una ruta con diez nodos.

Node.js 24 y Java 21 usan las instalaciones existentes. Maven 3.9.16 y MySQL estan en `.local`, excluida de Git. Las contrasenas generadas estan en `.local/env.ps1` y `.local/root.cnf`; no compartir esos archivos. Los datos persistentes estan en `.local/mysql-data`: no borrar esa carpeta. Los registros de ejecucion estan en `.local/*.log`.

El usuario SQL de la API solo tiene permiso SELECT, suficiente para los endpoints actuales de consulta. Las futuras funciones de escritura necesitaran permisos por dominio.

Para recompilar el backend tras editar Java, detiene Rupi y ejecuta el script de inicio con `-Recompilar`. Vite actualiza los cambios de React durante el desarrollo. Para validar la web: `npm.cmd run build` y `npm.cmd run lint`.

La autenticacion incluida en el repositorio es de demostracion: usa el UUID del estudiante. Este entorno es de desarrollo local. Django y Android todavia no tienen una aplicacion implementada; las historias HU-02 a HU-04 siguen pendientes.

Descarga oficial de MySQL: https://dev.mysql.com/downloads/mysql/8.4.html

Actualización: entra con el botón de demostración local. Ahora se valida una sesión de MySQL y se entrega una cookie HttpOnly; ya no se admite el UUID como credencial. Ver docs/implementacion/HU-01-verificacion-local.md para los límites y pruebas.

