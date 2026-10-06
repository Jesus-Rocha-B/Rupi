# Catálogo de Patrones "Diseño con IA" vs. Alternativas Artesanales

Este documento es una guía de contraste directo. Cuando una interfaz o componente empiece a parecer generado por una plantilla automatizada de IA, consulta esta tabla y aplica la alternativa artesanal.

---

## 1. Fondos y Superficies

### ❌ Patrón IA Genérico
* Fondos negros o grises azulados oscuros (`#0b0f19`, `#111827`) con orbes de luz difuminados (`filter: blur(120px)`) en esquinas aleatorias.
* Tarjetas transparentes con `backdrop-filter: blur(10px)` y bordes translúcidos de `1px solid rgba(255,255,255,0.1)`.

### ✅ Alternativa Artesanal y Situada
* Fondos con calidez terrenal y materialidad: tonos crema papel (`#fbf8f3`), lino o arcilla suave (`#eef5e8`).
* Superficies con contraste nítido, bordes sólidos bien calibrados (`1.5px solid #d4e2ca`), dando sensación de objeto físico, libreta o tablero de aventuras.
* Texturas sutiles con patrones SVG andinos (zigzag geométrico, líneas punteadas como caminos reales de tiza o tierra).

---

## 2. Botones y Elementos de Acción

### ❌ Patrón IA Genérico
* Botones con degradados lineares (`background: linear-gradient(135deg, #6366f1, #a855f7)`).
* Efecto hover consistente solo en cambiar de gradiente o lanzar un brillo exterior.
* Cero respuesta al clic real: la interacción se siente plana como tocar un vidrio inerte.

### ✅ Alternativa Artesanal
* **Efecto botón de relieve físico (Chunky / Toy button):**
  * Sombra inferior sólida (`box-shadow: 0 5px 0 #cc5910`).
  * Al presionar (`:active`), el botón se desplaza 3px hacia abajo y la sombra se reduce a 2px.
  * Sonido visual: da retroalimentación instantánea e inequívoca de que fue pulsado, ideal para niños de primaria.
* Bordes redondeados generosos (`border-radius: 14px` a `20px`) sin deformar la silueta ni lucir como píldoras hiperestiradas.

---

## 3. Tipografía y Jerarquía

### ❌ Patrón IA Genérico
* Todo en la misma fuente sin remates genérica (Inter, Roboto, Arial) con todos los textos en gris medio (`#6b7280`) y títulos en `text-4xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r`.
* Espaciados mecánicos que hacen que todo el contenido parezca una presentación de diapositivas genérica.

### ✅ Alternativa Artesanal
* Fuente display con carácter (Nunito en pesos 800 y 900) con trazos redondeados y amigables.
* Jerarquía clara con nombres expresivos:
  * Eyebrow / Antetítulo en mayúsculas pequeñas con tracking positivo: `letter-spacing: 0.08em; font-size: 11px; font-weight: 800; color: #43614c;`.
  * Título rotundo, cálido y legible.
  * Cuerpo de texto en un color con alto contraste (evitar grises tenues ilegibles en proyectores de aula).

---

## 4. Ilustraciones y Mascotas

### ❌ Patrón IA Genérico
* Avatares 3D hiperbrillantes de plástico generados en masa o ilustraciones estilo vector abstracto corporativo (personas sin rostro con extremidades azules gigantes).

### ✅ Alternativa Artesanal
* Personajes con identidad y contexto: **Rupi el gallito de las rocas**, con su cresta característica, plumaje rojo-anaranjado vivo y vestimenta o elementos que evocan la cultura y geografía peruana.
* Integración activa del personaje en la experiencia: da pistas, celebra logros y acompaña los estados de error con empatía ("¡Ánimo! Vamos a intentarlo juntos de nuevo").
