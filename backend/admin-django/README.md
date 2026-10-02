# Django · Administración

Aquí vivirá el backend administrativo. Su responsabilidad será administrar identidad, roles/permisos, versiones del catálogo curricular y configuración global de políticas/proveedores de IA.

## Reglas de integración

- Se conecta al esquema MySQL `rupi` definido en `../../database/mysql/`.
- Es el único escritor de sus dominios asignados; no modifica las tablas cuyo escritor es Spring Boot.
- Las migraciones del esquema compartido deben tener un propietario y un orden coordinados antes de habilitar despliegues.
- No guardar secretos en el repositorio. Usar variables de entorno en desarrollo y en el proveedor de despliegue.

El proyecto Django se inicializará cuando una historia necesite una función administrativa concreta.
