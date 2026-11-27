# RUPI

RUPI es una plataforma de aprendizaje digital para estudiantes de primaria de colegios públicos del Perú. El gallito de las rocas guía una experiencia con rutas interactivas y actividades relacionadas con el currículo y el contexto peruano.

## Estructura del monorepo

```text
D:/Rupi
├── frontend/
│   ├── web-react/          # Plataforma web (Docentes y Administración)
│   └── mobile-kotlin/      # Aplicación móvil Android (Estudiantes)
├── backend/
│   ├── admin-django/       # Administración de usuarios, roles y catálogo curricular
│   └── api-spring-boot/    # API de estudiantes, docentes y app móvil
├── database/mysql/         # Modelo y documentación MySQL
└── docs/                   # Historias y proceso de implementación
```

React web y Kotlin se conectarán a Spring Boot. La aplicación administrativa React se conectará a Django. Ambos servicios compartirán MySQL con propiedad de escritura por dominio, definida en [`database/mysql/README.md`](database/mysql/README.md). El modelo integral está en [`database/mysql/schema.sql`](database/mysql/schema.sql).

## Base de Datos Compartida

El equipo utiliza una base de datos centralizada en la nube (TiDB Cloud Serverless) para compartir usuarios, rutas y actividades escolares en tiempo real.
Consulta la guía de conexión en [`docs/BASE-DE-DATOS-COMPARTIDA.md`](docs/BASE-DE-DATOS-COMPARTIDA.md).

## Laboratorio 13 aplicado a Rupi

Requisitos, reglas, arquitectura, rutas y estructura adaptados al sistema educativo: [docs/LABORATORIO-13-RUPI.md](docs/LABORATORIO-13-RUPI.md).

## Implementación por historias

El plan detallado de las primeras cuatro historias está en [`docs/implementacion/README.md`](docs/implementacion/README.md). La HU-01, su contrato de API y su seguimiento están en [`docs/implementacion/HU-01-mapa-interactivo.md`](docs/implementacion/HU-01-mapa-interactivo.md).

---

## Arquitectura de Componentes y Diseño de Software (Semana 14)

### 1. Componentes del Sistema
- **Base de Datos:** MySQL 8.4 compartido (`database/mysql/schema.sql`) con modelo relacional integral dividido por dominios.
- **Backend Administrativo:** Django REST Framework (`backend/admin-django/`) con apps desacopladas:
  - `curriculo`: Catálogo curricular (grados, áreas, competencias) y paradas culturales de las rutas.
  - `identidad`: Cuentas de usuario, perfiles, roles escolares y políticas de acceso.
  - `operaciones`: Registro de eventos de auditoría y sincronización.
- **Backend de Servicio Educativo:** Spring Boot (`backend/api-spring-boot/`) para entrega reactiva del avance y contenido a los estudiantes.
- **Frontend Web:** React 19 + TypeScript + Vite (`frontend/web-react/`), estructurado en componentes reutilizables (`components/`) y pantallas por dominio (`features/roadmap`, `features/teacher`, `features/calendar`, `features/reports`).
- **Canal Móvil:** Aplicación Android nativa con Kotlin y Jetpack Compose (`frontend/mobile-kotlin/`).

### 2. Entidad Principal y Detalle Relacional
- **Entidad Principal:** `ContextoCultural` (`curriculo_contexto_cultural`), que modela el entorno geográfico y patrimonial peruano vinculado a las rutas pedagógicas.
- **Entidad de Detalle:** `ContextoCulturalDato` (`curriculo_contexto_cultural_dato`), que almacena los datos curiosos y píldoras formativas por cada parada con `on_delete=models.CASCADE`.
- **Entidades Maestras de Catálogo:** `VersionCatalogo`, `Grado` (1° a 6° de primaria), `Area` (Matemática, Comunicación) y `Unidad`.

### 3. Servicios Externos y Justificación de Ausencia de Pagos
De acuerdo con la naturaleza del proyecto y la normativa educativa escolar:
- **Ausencia de micropagos/comercio:** RUPI es una plataforma de software público orientada a estudiantes de primaria (6 a 11 años) y docentes de escuelas públicas. En cumplimiento estricto de las normativas de protección del menor (COPPA y Ley N° 29733 de Protección de Datos Personales del Perú), **la plataforma no incluye módulos de pago, carritos de compra ni micropagos**.
- **Servicio Externo Integrado (Almacenamiento Multimedia y CDN):**
  - **Momento de invocación:** Al cargar el mapa interactivo de rutas, reproducir las narraciones orales del gallito Rupi y descargar los paquetes `.zip` de actividades offline en zonas con baja conectividad.
  - **Datos que viajan al servicio externo:** Solicitudes GET con tokens de acceso seguro para streaming de audio MP3 e imágenes SVG/WebP.
  - **Datos almacenados en BD:** URL pública segura, hash SHA-256 de integridad y metadatos de autor/licencia cultural.
  - **Datos que NUNCA se guardan en el cliente:** Llaves secretas de administración del bucket/storage.

### 4. Mapa de Rutas Principales (API vs React)

| Recurso | Ruta API Backend | Ruta Frontend Web (React) | Propósito en RUPI |
| :--- | :--- | :--- | :--- |
| **Catálogo de Rutas** | `GET /api/v1/curriculo/rutas/` | `/rutas` | Listar las rutas pedagógicas disponibles filtradas por grado y área. |
| **Detalle de Ruta / Paradas** | `GET /api/v1/curriculo/rutas/{id}/` | `/rutas/:id` | Visualizar el mapa interactivo con las paradas del gallito Rupi y contexto cultural. |
| **Panel Docente** | `GET /api/v1/aulas/{id}/estudiantes/` | `/docente/aulas/:id` | Monitoreo del progreso y métricas de rendimiento del aula. |
| **Perfil y Logros** | `GET /api/v1/identidad/perfil/` | `/perfil` | Visualización de insignias ganadas y configuración del alias escolar. |

---

## Ejecución y Verificación del Proyecto

### Backend (Django Admin):
```bash
cd backend/admin-django
python manage.py check
python manage.py runserver 8000
```

### Frontend (React Web):
```bash
cd frontend/web-react
npm run lint
npm run build
npm run dev
```
