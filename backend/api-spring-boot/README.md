# Spring Boot · API de la plataforma

Aquí vivirá la API compartida por React web de estudiantes/docentes y la aplicación móvil Kotlin. Spring Boot será el escritor de escuelas/aulas, rutas y actividades, asignaciones, progreso, evaluaciones, gamificación y dominios operativos asignados.

## HU-01

La primera entrega implementará `GET /api/v1/student/learning-routes` y `GET /api/v1/student/learning-routes/{versionRouteId}` para consultar inscripciones, versiones publicadas de rutas, nodos y progreso desde MySQL `rupi`. El contrato de respuesta, autorización y estados de pantalla está en [`../../docs/implementacion/HU-01-mapa-interactivo.md`](../../docs/implementacion/HU-01-mapa-interactivo.md#contrato-inicial-de-api).

## Reglas de integración

- Modelo objetivo y esquema: `../../database/mysql/`.
- No modificar tablas propiedad de Django.
- No habilitar cambios automáticos del esquema por ORM en producción; coordinar las migraciones compartidas.
- Configurar credenciales y TLS mediante variables de entorno; no versionar secretos.

## Ejecutar localmente

Requiere Java 21, Maven 3.6.3 o superior y una instancia MySQL con `database/mysql/schema.sql` aplicado. Configura `RUPI_DB_URL`, `RUPI_DB_USERNAME` y `RUPI_DB_PASSWORD` en el entorno. La URL predeterminada apunta a `localhost:3306/rupi`; el servidor nunca ejecuta el DDL automáticamente.

```powershell
mvn spring-boot:run
```

Los endpoints de HU-01 ya consultan el esquema existente. El principal autenticado debe entregar como nombre el UUID de `identidad_cuenta_usuario`; sin principal válido la API responde 401. La integración con el mecanismo de sesión/autenticación de las historias de identidad se hará antes de habilitar acceso real.
