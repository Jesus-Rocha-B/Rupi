---
name: educational-game-ux
description: >-
  Guía especializada de UX, microinteracciones pedagógicas y diseño para estudiantes de primaria en colegios públicos del Perú. Evita la sobreestimulación visual, la infantilización vacía y los patrones oscuros de adicción; prioriza accesibilidad en pantallas táctiles de bajo costo, claridad de objetivos, reforzamiento positivo del avance escolar y la guía de la mascota Rupi. Usar al diseñar rutas, niveles, modales de lección, estados vacíos, feedback de aciertos/errores y flujos de aprendizaje.
---

# Educational Game UX · Aprendizaje y Gamificación Positiva

Esta habilidad guía el diseño de experiencias de aprendizaje para niñas y niños de 6 a 12 años en el entorno escolar peruano, asegurando que cada elemento refuerce el aprendizaje sin caer en distracciones ni en clichés de videojuegos comerciales adictivos.

---

## 1. Principios Pedagógicos de UX

### A. Baja Carga Cognitiva y Claridad
* **Una acción primaria clara por pantalla:** Cuando el estudiante ve la ruta, la acción obvia debe ser continuar su lección actual.
* **Mensajes en lenguaje cercano y respetuoso:** Sin condescendencia ni jerga técnica ("HTTP 500", "Token expirado").
* **Estructura predecible:** El mapa sigue una progresión natural de abajo hacia arriba o en zigzag numerado ascendente.

### B. Gamificación Sana (No Adictiva)
* **El progreso celebra el esfuerzo, no la competencia destructiva:**
  * No crear tablas de posiciones públicas obligatorias que avergüencen a estudiantes con menor ritmo.
  * Los logros son personales: *"¡Completaste 3 retos de operaciones en la feria!"*.
  * El cálculo de avance debe ser transparente (número de paradas completadas sobre el total).
* **Fallas como oportunidades de aprendizaje:**
  * Cuando un estudiante no acierta un ejercicio, el feedback no es un sonido de castigo estridente ni una cruz roja amenazante. Rupi ofrece una pista constructiva (*"Prueba separar las cantidades en decenas y unidades"*).

### C. Accesibilidad para Dispositivos del Entorno Escolar Peruano
* **Optimización para pantallas de bajo costo:**
  * Tabletas escolares del MINEDU o celulares de gama de entrada con bajo brillo o respuesta táctil lenta.
  * Objetivos de toque amplios: mínimo `48px` x `48px` con suficiente margen entre botones.
  * Contraste cromático estricto (evitar grises claros sobre blanco).
  * Compatibilidad total con navegación por teclado y lectores de pantalla.

---

## 2. El Rol del Compañero Tutor (Rupi)

* Rupi no es un pop-up invasivo ni una animación que bloquea la pantalla.
* Aparece como un amigo explorador:
  * Provee pistas útiles opcionales que el estudiante puede abrir o cerrar voluntariamente.
  * Da la bienvenida usando el nombre del estudiante obtenido de la sesión.
  * Ofrece consejos sobre cómo avanzar al propio ritmo.

---

## 3. Documentación Complementaria

* [`references/child-friendly-ux.md`](./references/child-friendly-ux.md) — Directrices de accesibilidad, tipografía y diseño cognitivo para la infancia.
