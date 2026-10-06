# Reglas y Estándares de Ingeniería · RUPI

Este archivo define las reglas de desarrollo y directrices de diseño que rigen todo el repositorio RUPI.

## 1. Diseño Frontend Humano y Artesanal (Anti-AI Slop)

* **Prohibido el aspecto genérico de diseño con IA:**
  * No emplear gradientes morados/azules genéricos ni halos difusos flotantes (`box-shadow: 0 0 40px rgba(...)`).
  * No usar plantillas impersonales estilo SaaS corporativo en un producto para niñas y niños de primaria peruana.
  * No generar textos robóticos o copys vacíos ("potencia tu aprendizaje con soluciones inteligentes").
* **Identidad y Materialidad:**
  * Mantener la identidad visual peruana inspirada en el gallito de las rocas (*Rupicola peruvianus*), la geografía andina y los materiales escolares.
  * Botones con volumen físico y respuesta táctil clara (`:active` con desplazamiento hacia abajo y reducción de sombra).
  * Paleta de colores oficial: Verde bosque (`#173e30`), Verde hoja (`#31724b`), Naranja Rupi (`#f95738`), Amarillo sol/maíz (`#ffd26a`) y Azul cielo (`#246a91`).
  * Consultar siempre la habilidad [`.agents/skills/human-crafted-frontend/`](.agents/skills/human-crafted-frontend/SKILL.md) antes de crear nuevos componentes visuales o pantallas.

## 2. Experiencia de Usuario Educativa y Accesibilidad Infantil

* Diseñado para estudiantes de primaria de 6 a 12 años en colegios públicos del Perú.
* Objetivos táctiles mínimos de `48px x 48px` para facilitar la interacción en tabletas escolares.
* Contraste cromático estricto (mínimo WCAG AA / AAA) para asegurar legibilidad en proyectores y pantallas con bajo brillo.
* Foco de navegación por teclado accesible y nítido (`:focus-visible`).
* Consultar la habilidad [`.agents/skills/educational-game-ux/`](.agents/skills/educational-game-ux/SKILL.md) al diseñar nuevas mecánicas de gamificación o rutas.

## 3. Integridad Arquitectónica y Base de Datos

* **Principio de Escritor Único (Single Writer):**
  * Spring Boot es el único escritor de `escuela_*`, `aprendizaje_*`, `evaluacion_*` y `gamificacion_*`.
  * Django es el único escritor de `identidad_*` y `curriculo_*`.
  * Ningún servicio modifica tablas que no le pertenecen.
* **Invariantes:**
  * El porcentaje y avance completado nunca se guardan duplicados en columnas fijas; se calculan en tiempo de consulta.
  * Los IDs son UUID `CHAR(36)` y las fechas se guardan en UTC `DATETIME(6)`.
* **Compatibilidad de Scripts:**
  * Todos los scripts de PowerShell deben ser compatibles tanto con Windows PowerShell 5.1 nativo como con PowerShell 7+.
