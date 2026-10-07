-- Solo datos educativos de demostración para la ruta HU-01. Requiere seed-hu01.sql.
-- No agrega matrículas ni desbloquea actividades ni modifica el avance de estudiantes.
USE rupi;
INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo)
SELECT CONCAT('bb000000-0000-4000-8000-', LPAD(n.numero_secuencia, 12, '0')),
       n.version_actividad_id, 1, 'TEXTO',
       CASE n.numero_secuencia
         WHEN 1 THEN 'Cuenta los puestos de una plaza: hay 4 junto a la fuente y 3 junto a los arcos. Dibuja ambos grupos y cuenta cuántos puestos hay en total.'
         WHEN 2 THEN 'Un retablo tiene 6 figuras y otro tiene 2. Para sumar, empieza en 6 y cuenta dos más: 7, 8. Practica con 5 figuras y 4 figuras.'
         WHEN 3 THEN 'En una canasta hay 9 panes. Si se venden 3, quedan 6: 9 menos 3 es 6. Dibuja 8 panes y tacha 2. ¿Cuántos quedan?'
         WHEN 4 THEN 'Una fruta cuesta 2 soles. Dos frutas cuestan 4 soles. Si llevas 5 soles, ¿cuánto recibirás de vuelto? Puedes dibujar las monedas.'
         WHEN 5 THEN 'Observa este patrón: rojo, amarillo, rojo, amarillo. El siguiente color es rojo. Crea en tu cuaderno otro patrón con dos formas.'
         WHEN 6 THEN 'Imagina un camino de piedra en la plaza. Mide un trozo con tus manos, una al lado de otra, y cuenta cuántas manos mide. Mide otro trozo y compara: ¿cuál es más largo? Si enrollas un sorbete sigue midiendo lo mismo.'
         WHEN 7 THEN 'Mira las piedras y los arcos de la plaza: ¿qué formas ves? Busca un círculo, un cuadrado y un triángulo, dibuja cada uno y cuenta sus lados. ¿Cuáles ruedan y cuáles no?'
         WHEN 8 THEN 'Las campanas anuncian los días de fiesta. Di los días de la semana en orden: lunes, martes, miércoles... Si hoy es martes, ¿qué día es mañana? Mira el horario de tu clase y cuenta cuántos días vas al colegio.'
         WHEN 9 THEN 'Camina del banco a la fuente contando tus pasos: uno, dos, tres... Si das 6 pasos hacia la fuente y 4 de regreso, ¿cuántos pasos diste en total? Compara con un amigo: ¿quién dio más pasos?'
         ELSE 'En la cosecha hay 12 papas en una canasta. Si se llevan 5, ¿cuántas quedan? Si luego agregas 3, ¿cuántas hay? Explica a tu docente cómo lo pensaste y compara tu respuesta con la de un amigo.'
       END
FROM aprendizaje_nodo_ruta n
WHERE n.version_ruta_id = 'b0000000-0000-4000-8000-000000000001'
  AND NOT EXISTS (SELECT 1 FROM aprendizaje_bloque_contenido b WHERE b.version_actividad_id = n.version_actividad_id AND b.numero_secuencia = 1);
