# RUPI

RUPI es una plataforma de aprendizaje digital para estudiantes de primaria de colegios públicos del Perú. El gallito de las rocas guía una experiencia con rutas interactivas y actividades relacionadas con el currículo y el contexto peruano.

## Estructura del monorepo

```text
D:/Rupi
├── frontend/
│   ├── web-react/          # Plataforma web
│   └── mobile-kotlin/      # Aplicación móvil Android
├── backend/
│   ├── admin-django/       # Administración de usuarios, roles y catálogo
│   └── api-spring-boot/    # API de estudiantes, docentes y app móvil
├── database/mysql/         # Modelo y documentación MySQL
└── docs/                   # Historias y proceso de implementación
```

React web y Kotlin se conectarán a Spring Boot. La aplicación administrativa React se conectará a Django. Ambos servicios compartirán MySQL con propiedad de escritura por dominio, definida en [`database/mysql/README.md`](database/mysql/README.md). El modelo integral está en [`database/mysql/schema.sql`](database/mysql/schema.sql).

## Ejecutar la web

Necesitas Node.js y npm. Desde la raíz:

```bash
npm install
npm run dev
```

También puedes ejecutar los comandos directamente desde `frontend/web-react`. La API Spring Boot ya contiene la primera consulta de HU-01, pero todavía debe conectarse con la autenticación y una instancia MySQL con datos de desarrollo. El proyecto móvil Kotlin se incorporará cuando avancemos sus historias.

## Compilar y revisar la web

```bash
npm run build
npm run lint
```

## Implementación por historias

El plan detallado de las primeras cuatro historias está en [`docs/implementacion/README.md`](docs/implementacion/README.md). La HU-01, su contrato de API y su seguimiento están en [`docs/implementacion/HU-01-mapa-interactivo.md`](docs/implementacion/HU-01-mapa-interactivo.md).
