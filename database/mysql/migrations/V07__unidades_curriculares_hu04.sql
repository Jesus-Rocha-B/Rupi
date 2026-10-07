-- V07: Currículo oficial de Matemática de 2.° de primaria y unidades de demostración (HU-04).
-- Requiere V04 y seed-hu01.sql (catálogo, área MAT, 2.° grado y la ruta de diez nodos).
-- Idempotente: se puede aplicar más de una vez y no toca el progreso de ningún estudiante.
--
-- QUÉ ES OFICIAL (MINEDU, Currículo Nacional de la Educación Básica):
--   Las 4 competencias de Matemática, sus capacidades, el estándar de aprendizaje del ciclo III
--   (nivel 3, final de 2.°) y los desempeños de 2.° grado. Texto transcrito del «Programa curricular
--   de Educación Primaria» (RM 281-2016-MINEDU, modificada por RM 159-2017-MINEDU), pp. 236-237
--   (cantidad), 246-247 (regularidad), 256-257 (forma) y 266 (datos). Fuente completa y
--   fecha de consulta en docs/implementacion/HU-04-fuentes-curriculares.md.
-- QUÉ ES DEMOSTRACIÓN:
--   El MINEDU no publica «unidades» para primaria: las programa cada docente o colegio. Las tres
--   unidades (código DEMO-*) agrupan los diez nodos de la ruta y enlazan los estándares y desempeños
--   oficiales que trabaja cada tramo. La carga de unidades reales queda para Django (dueño de curriculo_*).
-- UTF-8 / MySQL 8.4 InnoDB

USE rupi;
SET NAMES utf8mb4;

-- 1. Procedencia correcta del catálogo (la semilla antigua decía «CNEB-2024» sin fuente)
UPDATE curriculo_version_catalogo
SET etiqueta_version = 'CNEB-2017',
    referencia_origen = 'Currículo Nacional de la Educación Básica, MINEDU (RM 281-2016-MINEDU, modificada por RM 159-2017-MINEDU). Programa curricular de Educación Primaria.',
    vigente_desde = '2017-01-01'
WHERE id = 'c0000000-0000-4000-8000-000000000001';

-- 2. Competencias de Matemática con sus capacidades
INSERT INTO curriculo_competencia (id, area_id, codigo, nombre, descripcion) VALUES
  ('c1000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'MAT-C1', 'Resuelve problemas de cantidad',
   'Capacidades: Traduce cantidades a expresiones numéricas; Comunica su comprensión sobre los números y las operaciones; Usa estrategias y procedimientos de estimación y cálculo; Argumenta afirmaciones sobre las relaciones numéricas y las operaciones.'),
  ('c1000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'MAT-C2', 'Resuelve problemas de regularidad, equivalencia y cambio',
   'Capacidades: Traduce datos y condiciones a expresiones algebraicas y gráficas; Comunica su comprensión sobre las relaciones algebraicas; Usa estrategias y procedimientos para encontrar equivalencias y reglas generales; Argumenta afirmaciones sobre relaciones de cambio y equivalencia.'),
  ('c1000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'MAT-C3', 'Resuelve problemas de forma, movimiento y localización',
   'Capacidades: Modela objetos con formas geométricas y sus transformaciones; Comunica su comprensión sobre las formas y relaciones geométricas; Usa estrategias y procedimientos para orientarse en el espacio; Argumenta afirmaciones sobre relaciones geométricas.'),
  ('c1000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001', 'MAT-C4', 'Resuelve problemas de gestión de datos e incertidumbre',
   'Capacidades: Representa datos con gráficos y medidas estadísticas o probabilísticas; Comunica su comprensión de los conceptos estadísticos y probabilísticos; Usa estrategias y procedimientos para recopilar y procesar datos; Sustenta conclusiones o decisiones con base en la información obtenida.')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

-- 3. Estándar de aprendizaje: descripción del nivel esperado al final del ciclo III (grado NULL: vale para el ciclo)
INSERT INTO curriculo_estandar (id, competencia_id, grado_id, codigo, descripcion) VALUES
  ('e1000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001', NULL, 'MAT-C1-CICLO-III',
   'Resuelve problemas referidos a acciones de juntar, separar, agregar, quitar, igualar y comparar cantidades; y las traduce a expresiones de adición y sustracción, doble y mitad. Expresa su comprensión del valor de posición en números de dos cifras y los representa mediante equivalencias entre unidades y decenas. Así también, expresa mediante representaciones su comprensión del doble y mitad de una cantidad; usa lenguaje numérico. Emplea estrategias diversas y procedimientos de cálculo y comparación de cantidades; mide y compara el tiempo y la masa, usando unidades no convencionales. Explica por qué debe sumar o restar en una situación y su proceso de resolución.'),
  ('e1000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000002', NULL, 'MAT-C2-CICLO-III',
   'Resuelve problemas que presentan equivalencias o regularidades, traduciéndolas a igualdades que contienen operaciones de adición o de sustracción y a patrones de repetición de dos criterios perceptuales y patrones aditivos. Expresa su comprensión de las equivalencias y de cómo es un patrón, usando material concreto y diversas representaciones. Emplea estrategias, la descomposición de números, cálculos sencillos para encontrar equivalencias, o para continuar y crear patrones. Explica las relaciones que encuentra en los patrones y lo que debe hacer para mantener el “equilibrio” o la igualdad, con base en experiencias y ejemplos concretos.'),
  ('e1000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000003', NULL, 'MAT-C3-CICLO-III',
   'Resuelve problemas en los que modela las características y datos de ubicación de los objetos del entorno a formas bidimensionales y tridimensionales, sus elementos, posición y desplazamientos. Describe estas formas mediante sus elementos: número de lados, esquinas, lados curvos y rectos; número de puntas caras, formas de sus caras, usando representaciones concretas y dibujos. Así también traza y describe desplazamientos y posiciones, en cuadriculados y puntos de referencia usando algunos términos del lenguaje geométrico. Emplea estrategias y procedimientos basados en la manipulación, para construir objetos y medir su longitud (ancho y largo) usando unidades no convencionales. Explica semejanzas y diferencias entre formas geométricas, así como su proceso de resolución.'),
  ('e1000000-0000-4000-8000-000000000004', 'c1000000-0000-4000-8000-000000000004', NULL, 'MAT-C4-CICLO-III',
   'Resuelve problemas relacionados con datos cualitativos en situaciones de su interés, recolecta datos a través de preguntas sencillas, los registra en listas o tablas de conteo simple (frecuencia) y los organiza en pictogramas horizontales y gráficos de barras simples. Lee la información contenida en estas tablas o gráficos identificando el dato o datos que tuvieron mayor o menor frecuencia y explica sus decisiones basándose en la información producida. Expresa la ocurrencia de sucesos cotidianos usando las nociones de posible o imposible y justifica su respuesta.')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- 4. Desempeños de 2.° grado (cada uno es una meta de aprendizaje)
INSERT INTO curriculo_meta_aprendizaje (id, competencia_id, grado_id, codigo, descripcion) VALUES
  ('f1000000-0000-4000-8000-000000000101', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D1',
   'Establece relaciones entre datos y una o más acciones de agregar, quitar, avanzar, retroceder, juntar, separar, comparar e igualar cantidades, y las transforma en expresiones numéricas (modelo) de adición o sustracción con números naturales de hasta dos cifras.'),
  ('f1000000-0000-4000-8000-000000000102', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D2',
   'Expresa con diversas representaciones y lenguaje numérico (números, signos y expresiones verbales) su comprensión de la decena como nueva unidad en el sistema de numeración decimal y el valor posicional de una cifra en números de hasta dos cifras.'),
  ('f1000000-0000-4000-8000-000000000103', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D3',
   'Expresa con diversas representaciones y lenguaje numérico (números, signos y expresiones verbales) su comprensión del número como ordinal al ordenar objetos hasta el vigésimo lugar, de la comparación entre números y de las operaciones de adición y sustracción, el doble y la mitad, con números de hasta dos cifras.'),
  ('f1000000-0000-4000-8000-000000000104', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D4',
   'Emplea estrategias y procedimientos como los siguientes: estrategias heurísticas; estrategias de cálculo mental, como las descomposiciones aditivas o el uso de analogías (70 + 20; 70 + 9, completar a la decena más cercana, usar dobles, sumar en vez de restar, uso de la conmutatividad); procedimientos de cálculo, como sumas o restas con y sin canjes; estrategias de comparación, que incluyen el uso del tablero cien y otros.'),
  ('f1000000-0000-4000-8000-000000000105', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D5',
   'Compara en forma vivencial y concreta la masa de objetos usando unidades no convencionales, y mide el tiempo usando unidades convencionales (días, horarios semanales).'),
  ('f1000000-0000-4000-8000-000000000106', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D6',
   'Realiza afirmaciones sobre la comparación de números naturales y de la decena, y las explica con material concreto.'),
  ('f1000000-0000-4000-8000-000000000107', 'c1000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'MAT-C1-2G-D7',
   'Realiza afirmaciones sobre por qué debe sumar o restar en un problema y las explica; así también, explica su proceso de resolución y los resultados obtenidos.'),
  ('f1000000-0000-4000-8000-000000000201', 'c1000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'MAT-C2-2G-D1',
   'Establece relaciones de equivalencias entre dos grupos de hasta veinte objetos y las transforma en igualdades que contienen adiciones o sustracciones.'),
  ('f1000000-0000-4000-8000-000000000202', 'c1000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'MAT-C2-2G-D2',
   'Establece relaciones entre los datos que se repiten (objetos, colores, diseños, sonidos o movimientos) o entre cantidades que aumentan o disminuyen regularmente, y los transforma en patrones de repetición o patrones aditivos.'),
  ('f1000000-0000-4000-8000-000000000203', 'c1000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'MAT-C2-2G-D3',
   'Expresa, con lenguaje cotidiano y representaciones concretas o dibujos, su comprensión de la equivalencia como equilibrio o igualdad entre dos colecciones o cantidades.'),
  ('f1000000-0000-4000-8000-000000000204', 'c1000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'MAT-C2-2G-D4',
   'Describe, usando lenguaje cotidiano y representaciones concretas y dibujos, el patrón de repetición (con dos criterios perceptuales), y cómo aumentan o disminuyen los números en un patrón aditivo con números de hasta 2 cifras.'),
  ('f1000000-0000-4000-8000-000000000205', 'c1000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002', 'MAT-C2-2G-D5',
   'Emplea estrategias heurísticas y estrategias de cálculo (el conteo o la descomposición aditiva) para encontrar equivalencias, mantener la igualdad (“equilibrio”) o crear, continuar y completar patrones.'),
  ('f1000000-0000-4000-8000-000000000301', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D1',
   'Establece relaciones entre las características de los objetos del entorno, las asocia y representa con formas geométricas tridimensionales (cuerpos que ruedan y no ruedan) y bidimensionales (cuadrado, rectángulo, círculo, triángulo), así como con las medidas de su longitud (largo y ancho).'),
  ('f1000000-0000-4000-8000-000000000302', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D2',
   'Establece relaciones entre los datos de ubicación y recorrido de objetos y personas del entorno, y los expresa con material concreto y bosquejos o gráficos, posiciones y desplazamientos, teniendo en cuenta puntos de referencia en las cuadrículas.'),
  ('f1000000-0000-4000-8000-000000000303', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D3',
   'Expresa con material concreto y dibujos su comprensión sobre algún elemento de las formas tridimensionales (número de puntas, número de caras, formas de sus caras) y bidimensionales (número de lados, vértices, lados curvos y rectos). Asimismo, describe si los objetos ruedan, se sostienen, no se sostienen o tienen puntas o esquinas usando lenguaje cotidiano y algunos términos geométricos.'),
  ('f1000000-0000-4000-8000-000000000304', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D4',
   'Expresa con material concreto su comprensión sobre la medida de la longitud al determinar cuántas veces es más largo un objeto con relación a otro. Expresa también que el objeto mantiene su longitud a pesar de sufrir transformaciones como romper, enrollar o flexionar (conservación de la longitud).'),
  ('f1000000-0000-4000-8000-000000000305', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D5',
   'Emplea estrategias, recursos y procedimientos basados en la manipulación y visualización, para construir objetos y medir su longitud usando unidades no convencionales (manos, pasos, pies, etc.).'),
  ('f1000000-0000-4000-8000-000000000306', 'c1000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002', 'MAT-C3-2G-D6',
   'Hace afirmaciones sobre las semejanzas y diferencias entre las formas geométricas, y las explica con ejemplos concretos y con base en sus conocimientos matemáticos. Asimismo, explica el proceso seguido.'),
  ('f1000000-0000-4000-8000-000000000401', 'c1000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002', 'MAT-C4-2G-D1',
   'Representa las características y el comportamiento de datos cualitativos (por ejemplo, color de los ojos: pardos, negros; plato favorito: cebiche, arroz con pollo, etc.) de una población, a través de pictogramas horizontales (el símbolo representa una o dos unidades) y gráficos de barras verticales simples (sin escala), en situaciones cotidianas de su interés personal o de sus pares.'),
  ('f1000000-0000-4000-8000-000000000402', 'c1000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002', 'MAT-C4-2G-D2',
   'Expresa la ocurrencia de acontecimientos cotidianos usando las nociones “posible” e “imposible”.'),
  ('f1000000-0000-4000-8000-000000000403', 'c1000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002', 'MAT-C4-2G-D3',
   'Lee información contenida en tablas de frecuencia simple (conteo simple), pictogramas horizontales y gráficos de barras verticales simples; indica la mayor o menor frecuencia y compara los datos, los cuales representa con material concreto y gráfico.'),
  ('f1000000-0000-4000-8000-000000000404', 'c1000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002', 'MAT-C4-2G-D4',
   'Recopila datos mediante preguntas y el empleo de procedimientos y recursos (material concreto y otros); los procesa y organiza en listas de datos o tablas de frecuencia simple (conteo simple) para describirlos.'),
  ('f1000000-0000-4000-8000-000000000405', 'c1000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002', 'MAT-C4-2G-D5',
   'Toma decisiones sencillas y las explica a partir de la información obtenida.')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- 5. Tres unidades de demostración de 2.° grado, mismo catálogo y misma área que la ruta
INSERT INTO curriculo_unidad (id, version_catalogo_id, grado_id, area_id, codigo, titulo, numero_secuencia) VALUES
  ('d1000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'DEMO-MAT2-U1', 'Unidad 1: contamos, sumamos y restamos', 1),
  ('d1000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'DEMO-MAT2-U2', 'Unidad 2: patrones, medidas y formas', 2),
  ('d1000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'DEMO-MAT2-U3', 'Unidad 3: tiempo, pasos y retos', 3)
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo), numero_secuencia = VALUES(numero_secuencia);

-- 6. Qué competencias, estándares y desempeños oficiales trabaja cada unidad (se rehacen en cada aplicación)
DELETE FROM curriculo_unidad_competencia WHERE unidad_id IN ('d1000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000003');
DELETE FROM curriculo_unidad_estandar WHERE unidad_id IN ('d1000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000003');
DELETE FROM curriculo_unidad_meta WHERE unidad_id IN ('d1000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000003');

INSERT INTO curriculo_unidad_competencia (unidad_id, competencia_id) VALUES
  ('d1000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001'),
  ('d1000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000002'),
  ('d1000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000003'),
  ('d1000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000001'),
  ('d1000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000003');

INSERT INTO curriculo_unidad_estandar (unidad_id, estandar_id) VALUES
  ('d1000000-0000-4000-8000-000000000001', 'e1000000-0000-4000-8000-000000000001'),
  ('d1000000-0000-4000-8000-000000000002', 'e1000000-0000-4000-8000-000000000002'),
  ('d1000000-0000-4000-8000-000000000002', 'e1000000-0000-4000-8000-000000000003'),
  ('d1000000-0000-4000-8000-000000000003', 'e1000000-0000-4000-8000-000000000001'),
  ('d1000000-0000-4000-8000-000000000003', 'e1000000-0000-4000-8000-000000000003');

INSERT INTO curriculo_unidad_meta (unidad_id, meta_id) VALUES
  ('d1000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000101'),
  ('d1000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000103'),
  ('d1000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000104'),
  ('d1000000-0000-4000-8000-000000000001', 'f1000000-0000-4000-8000-000000000107'),
  ('d1000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000202'),
  ('d1000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000204'),
  ('d1000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000301'),
  ('d1000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000303'),
  ('d1000000-0000-4000-8000-000000000002', 'f1000000-0000-4000-8000-000000000304'),
  ('d1000000-0000-4000-8000-000000000003', 'f1000000-0000-4000-8000-000000000101'),
  ('d1000000-0000-4000-8000-000000000003', 'f1000000-0000-4000-8000-000000000105'),
  ('d1000000-0000-4000-8000-000000000003', 'f1000000-0000-4000-8000-000000000107'),
  ('d1000000-0000-4000-8000-000000000003', 'f1000000-0000-4000-8000-000000000305');

-- 7. Vincular los nodos a su unidad: paradas 1–4, 5–7 y 8–10.
--    Solo completa nodos sin unidad; no sobrescribe una asignación hecha después.
UPDATE aprendizaje_nodo_ruta
SET unidad_id = CASE
      WHEN numero_secuencia BETWEEN 1 AND 4 THEN 'd1000000-0000-4000-8000-000000000001'
      WHEN numero_secuencia BETWEEN 5 AND 7 THEN 'd1000000-0000-4000-8000-000000000002'
      ELSE 'd1000000-0000-4000-8000-000000000003'
    END
WHERE version_ruta_id = 'b0000000-0000-4000-8000-000000000001'
  AND unidad_id IS NULL;

-- 8. Vincular cada versión de actividad con la unidad de su nodo
INSERT IGNORE INTO aprendizaje_actividad_unidad (version_actividad_id, unidad_id)
SELECT n.version_actividad_id, n.unidad_id
FROM aprendizaje_nodo_ruta n
WHERE n.version_ruta_id = 'b0000000-0000-4000-8000-000000000001'
  AND n.unidad_id IS NOT NULL;

-- 9. Contenido de las paradas 6 a 10 alineado con su título y con los desempeños oficiales que trabaja su unidad
--    (la semilla antigua traía textos que no coincidían con el título). Datos de demostración, sin validar con un docente.
UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = 'Imagina un camino de piedra en la plaza. Mide un trozo con tus manos, una al lado de otra, y cuenta cuántas manos mide. Mide otro trozo y compara: ¿cuál es más largo? Si enrollas un sorbete sigue midiendo lo mismo.'
WHERE version_actividad_id = 'fa000000-0000-4000-8000-000000000006' AND numero_secuencia = 1;
UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = 'Mira las piedras y los arcos de la plaza: ¿qué formas ves? Busca un círculo, un cuadrado y un triángulo, dibuja cada uno y cuenta sus lados. ¿Cuáles ruedan y cuáles no?'
WHERE version_actividad_id = 'fa000000-0000-4000-8000-000000000007' AND numero_secuencia = 1;
UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = 'Las campanas anuncian los días de fiesta. Di los días de la semana en orden: lunes, martes, miércoles... Si hoy es martes, ¿qué día es mañana? Mira el horario de tu clase y cuenta cuántos días vas al colegio.'
WHERE version_actividad_id = 'fa000000-0000-4000-8000-000000000008' AND numero_secuencia = 1;
UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = 'Camina del banco a la fuente contando tus pasos: uno, dos, tres... Si das 6 pasos hacia la fuente y 4 de regreso, ¿cuántos pasos diste en total? Compara con un amigo: ¿quién dio más pasos?'
WHERE version_actividad_id = 'fa000000-0000-4000-8000-000000000009' AND numero_secuencia = 1;
UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = 'En la cosecha hay 12 papas en una canasta. Si se llevan 5, ¿cuántas quedan? Si luego agregas 3, ¿cuántas hay? Explica a tu docente cómo lo pensaste y compara tu respuesta con la de un amigo.'
WHERE version_actividad_id = 'fa000000-0000-4000-8000-000000000010' AND numero_secuencia = 1;
