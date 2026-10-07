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
         WHEN 6 THEN 'Busca un círculo, un cuadrado y un triángulo entre los objetos de tu casa. Dibuja cada uno y cuenta sus lados.'
         WHEN 7 THEN 'Agrupa 10 semillas: has formado una decena. Si añades 3 semillas tienes 13, una decena y tres unidades. Representa el número 15.'
         WHEN 8 THEN 'Compara 12 y 15: ambos tienen una decena, pero 5 unidades son más que 2. Por eso 15 es mayor que 12. Compara ahora 14 y 11.'
         WHEN 9 THEN 'Tres canastas tienen dos papas cada una. Puedes sumar 2 + 2 + 2 para encontrar 6 papas. Dibuja cuatro canastas con dos papas.'
         ELSE 'Repasa a tu ritmo: cuenta 10 objetos, separa 4 y explica cuántos quedan. Cuenta cómo encontraste la respuesta a tu docente.'
       END
FROM aprendizaje_nodo_ruta n
WHERE n.version_ruta_id = 'b0000000-0000-4000-8000-000000000001'
  AND NOT EXISTS (SELECT 1 FROM aprendizaje_bloque_contenido b WHERE b.version_actividad_id = n.version_actividad_id AND b.numero_secuencia = 1);
