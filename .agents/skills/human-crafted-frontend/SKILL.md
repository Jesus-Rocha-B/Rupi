---
name: human-crafted-frontend
description: >-
  Guía integral y estándares de diseño y desarrollo frontend para crear interfaces humanas, auténticas, táctiles y con identidad cultural única, erradicando por completo el aspecto genérico de "diseño generado por IA" (AI slop, fondos oscuros con resplandores morados/azules, plantillas SaaS clónicas, tarjetas flotantes sin peso y tipografías genéricas). Usar siempre que se diseñe, estilice o implemente interfaz de usuario, CSS, componentes React, paletas cromáticas, microinteracciones o maquetación web.
---

# Human-Crafted Frontend · Diseño Humano y Artesanal

Esta habilidad instruye al agente para construir interfaces de usuario que transmiten intención humana, artesanía visual, coherencia cultural y tacto físico real, evitando activamente los clichés del software diseñado o sugerido por inteligencias artificiales genéricas.

---

## 1. Los 6 Pecados del "Diseño con IA" y su Solución Humana

| Síntoma de Diseño con IA | Por qué ocurre | Solución Humana y Artesanal |
|---|---|---|
| **Resplandores morados/cian y gradientes cósmicos** | Prompts genéricos de "modern futuristic UI". | **Paleta situada y orgánica:** Colores con identidad del contexto real (tierra, arcilla, sol andino, bosque, pigmentos naturales). Sombras sólidas o directas, no halos fluorescentes. |
| **Tarjetas flotantes sin peso (`box-shadow: 0 10px 30px rgba(0,0,0,0.08)`)** | Falta de noción de materialidad en modelos de lenguaje. | **Tacto físico y bordes nítidos:** Bordes visibles de 1px a 2px (`border: 2px solid var(--line)`), fondos sólidos con textura o contraste nítido, y botones estilo bloque con profundidad (`box-shadow: 0 4px 0 #color-oscuro`). |
| **Tipografía robótica y sin jerarquía** | Uso por defecto de Inter, Roboto o System Sans sin calibración. | **Tipografía con carácter y propósito:** Tipografías display expresivas y cálidas (como Nunito 800/900 con esquinas amigables para niños), escala modular intencional y contrastes claros de peso y escala. |
| **Tarjetas idénticas en grilla 3x3** | Layouts seguros y repetitivos generados por plantillas. | **Composición asimétrica y narrativa:** Zonas destacadas, caminos serpenteantes (como el mapa de RUPI), paneles de compañía, hitos visuales y variación de ritmo visual. |
| **Microinteracciones desconectadas de la física** | Efectos `hover` que solo cambian opacidad o rotan iconos. | **Física de juguete / tactile feedback:** Botones que se hunden físicamente al hacer clic (`:active { transform: translateY(2px); box-shadow: 0 2px 0 ... }`), foco con anillo visible de 3px y rebotes elásticos naturales. |
| **Copys fríos y corporativos ("Potencia tu aprendizaje...")** | Lenguaje de marketing SaaS automatizado. | **Voz auténtica, cálida y empática:** Mensajes breves, motivadores y en el tono natural de la audiencia (niños peruanos de primaria y sus docentes). |

---

## 2. Principios de Artesanía Visual para RUPI

### A. Materialidad y Sensación Táctil (Toy / Boardgame UI)
La interfaz no debe sentirse como una pantalla abstracta, sino como un **juego de mesa o libro interactivo**:
* **Botones con volumen 3D real:**
  ```css
  .btn-tactile {
    background: #ff7f2a;
    color: #fff;
    border: none;
    border-radius: 16px;
    padding: 14px 24px;
    font-weight: 800;
    box-shadow: 0 5px 0 #cc5910;
    transition: transform 0.08s ease, box-shadow 0.08s ease;
    cursor: pointer;
  }
  .btn-tactile:hover {
    transform: translateY(-2px);
    box-shadow: 0 7px 0 #cc5910;
  }
  .btn-tactile:active {
    transform: translateY(3px);
    box-shadow: 0 2px 0 #cc5910;
  }
  ```
* **Foco accesible visible y amigable:**
  ```css
  :focus-visible {
    outline: 3px solid #246a91;
    outline-offset: 3px;
  }
  ```

### B. Paleta Cromática y Textura Cultural
Inspirada en el gallito de las rocas (*Rupicola peruvianus*), la geografía andina y los materiales escolares:
* **Verde Bosque Nuboso:** `#173e30` (Textos principales, solidez institucional).
* **Verde Hoja Andina:** `#31724b` (Avance, éxito, acentos positivos).
* **Naranja Gallito de las Rocas:** `#f95738` / `#ff7043` (Acciones principales, energía, personaje Rupi).
* **Amarillo Sol / Maíz:** `#ffd26a` / `#f4a261` (Monedas, estrellas, caminos, calidez).
* **Azul Cielo Serrano:** `#246a91` (Paradas disponibles, rutas de exploración).
* **Fondo Papel / Pergamino:** `#faf8f5` / `#f3efe6` (Descanso visual, emula cuadernos escolares).

### C. Espaciado y Escala
* **Tamaños de toque generosos:** En primaria, los dedos en tabletas o pantallas táctiles requieren mínimo `48px x 48px` en elementos interactivos.
* **Separación generosa entre paradas:** Evitar apiñar elementos. Permitir que el fondo respire.

---

## 3. Lista de Verificación "Anti-IA" antes de Aprobar un Componente

- [ ] ¿Tiene personalidad propia y se siente diseñado específicamente para este proyecto?
- [ ] ¿Evita sombras borrosas desmedidas y gradientes morados/azules genéricos?
- [ ] ¿Tiene respuesta táctil inmediata (`:hover`, `:active`, `:focus-visible`)?
- [ ] ¿Es accesible y legible en pantallas de bajo brillo (WCAG AA o superior)?
- [ ] ¿El texto es directo, humano, acogedor y sin tecnicismos innecesarios?
- [ ] ¿Los estados vacíos, de error y de carga aportan tranquilidad y dirección clara?

---

## 4. Archivos de Referencia

* [`references/anti-ai-patterns.md`](./references/anti-ai-patterns.md) — Catálogo detallado de patrones prohibidos y sus reemplazos humanos.
* [`references/peruvian-design-tokens.md`](./references/peruvian-design-tokens.md) — Ficha técnica de variables CSS y diseño temático para RUPI.
* [`examples/tactile-components.css`](./examples/tactile-components.css) — Ejemplos listos de botones, tarjetas de ruta y diálogos accesibles.
