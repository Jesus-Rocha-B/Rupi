-- HU-51. Actualizar la correspondencia curricular ante resoluciones ministeriales anuales.
-- Django es el único escritor de estas tablas (curriculo_*). Spring solo las lee para actualizar las rutas.
-- Cada resolución crea una versión BORRADOR copiada de la vigente; la correspondencia dice qué pasó con cada
-- área, competencia y unidad: se mantiene, se modifica o se retira. Lo agregado en el borrador se calcula
-- como NUEVO al consultar (no tiene origen). Al publicar, la versión anterior queda RETIRADA como registro.
CREATE TABLE IF NOT EXISTS curriculo_resolucion (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 numero VARCHAR(80) NOT NULL,
 fecha_emision DATE NOT NULL,
 resumen VARCHAR(600) NOT NULL,
 enlace_oficial VARCHAR(500) NULL,
 version_origen_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 version_destino_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 publicado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 publicado_en DATETIME(6) NULL,
 UNIQUE KEY uq_resolucion_numero (numero),
 UNIQUE KEY uq_resolucion_destino (version_destino_id),
 KEY ix_resolucion_origen (version_origen_id),
 FOREIGN KEY (version_origen_id) REFERENCES curriculo_version_catalogo(id),
 FOREIGN KEY (version_destino_id) REFERENCES curriculo_version_catalogo(id),
 FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
 FOREIGN KEY (publicado_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_correspondencia (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 resolucion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 tipo VARCHAR(20) NOT NULL,
 origen_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 -- Mientras la resolución está en borrador apunta a la copia; al publicar, lo RETIRADO queda en NULL.
 destino_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 cambio VARCHAR(20) NOT NULL DEFAULT 'SE_MANTIENE',
 nota VARCHAR(500) NULL,
 actualizado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_correspondencia_origen (resolucion_id,tipo,origen_id),
 UNIQUE KEY uq_correspondencia_destino (resolucion_id,tipo,destino_id),
 FOREIGN KEY (resolucion_id) REFERENCES curriculo_resolucion(id),
 FOREIGN KEY (actualizado_por) REFERENCES identidad_cuenta_usuario(id),
 CHECK (tipo IN ('AREA','COMPETENCIA','CAPACIDAD','UNIDAD','META','ESTANDAR')),
 CHECK (cambio IN ('SE_MANTIENE','MODIFICADA','RETIRADA')),
 CHECK (destino_id IS NOT NULL OR cambio = 'RETIRADA')
) ENGINE=InnoDB;
