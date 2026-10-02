-- RUPI: modelo relacional integral para las historias de usuario 1 a 60.
-- MySQL 8.4, InnoDB. El modelo completo se implementara por migraciones de dominio.

CREATE DATABASE IF NOT EXISTS rupi
  CHARACTER SET utf8mb4;
USE rupi;

CREATE TABLE IF NOT EXISTS identidad_cuenta_usuario (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  nombre_usuario VARCHAR(80) NOT NULL UNIQUE,
  correo VARCHAR(254) NULL UNIQUE,
  hash_clave VARCHAR(255) NOT NULL,
  estado ENUM('INVITADO','ACTIVO','BLOQUEADO','DESHABILITADO') NOT NULL DEFAULT 'INVITADO',
  bloqueado_hasta DATETIME(6) NULL,
  clave_cambiada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  desactivado_en DATETIME(6) NULL,
  CHECK ((estado = 'DESHABILITADO') = (desactivado_en IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_perfil_usuario (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  nombre VARCHAR(100) NULL,
  apellido VARCHAR(100) NULL,
  nombre_preferido VARCHAR(100) NULL,
  alias_publico VARCHAR(40) NULL,
  mostrar_en_clasificacion BOOLEAN NOT NULL DEFAULT FALSE,
  idioma VARCHAR(12) NOT NULL DEFAULT 'es-PE',
  zona_horaria VARCHAR(64) NOT NULL DEFAULT 'America/Lima',
  UNIQUE KEY uq_perfil_alias (alias_publico),
  CHECK (NOT mostrar_en_clasificacion OR alias_publico IS NOT NULL),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_version_catalogo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  etiqueta_version VARCHAR(60) NOT NULL UNIQUE,
  referencia_origen VARCHAR(255) NULL,
  vigente_desde DATE NULL,
  estado ENUM('BORRADOR','ACTIVO','RETIRADO') NOT NULL DEFAULT 'BORRADOR',
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_grado (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  numero_grado TINYINT UNSIGNED NOT NULL UNIQUE,
  nombre VARCHAR(80) NOT NULL,
  CHECK (numero_grado BETWEEN 1 AND 6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_area (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_catalogo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  UNIQUE KEY uq_area_version_codigo (version_catalogo_id, codigo),
  UNIQUE KEY uq_area_id_version (id, version_catalogo_id),
  FOREIGN KEY (version_catalogo_id) REFERENCES curriculo_version_catalogo(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_competencia (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  area_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(60) NOT NULL,
  nombre VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  UNIQUE KEY uq_competencia_area_codigo (area_id, codigo),
  FOREIGN KEY (area_id) REFERENCES curriculo_area(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_unidad (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_catalogo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  area_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(60) NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  numero_secuencia SMALLINT UNSIGNED NULL,
  UNIQUE KEY uq_unidad_version_grado_area_codigo (version_catalogo_id, grado_id, area_id, codigo),
  UNIQUE KEY uq_unidad_id_version_area (id, version_catalogo_id, area_id),
  FOREIGN KEY (version_catalogo_id) REFERENCES curriculo_version_catalogo(id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id),
  FOREIGN KEY (area_id, version_catalogo_id) REFERENCES curriculo_area(id, version_catalogo_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_unidad_competencia (
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  competencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (unidad_id, competencia_id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id),
  FOREIGN KEY (competencia_id) REFERENCES curriculo_competencia(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS recurso_archivo_multimedia (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  proveedor_almacenamiento VARCHAR(40) NULL,
  clave_almacenamiento VARCHAR(500) NULL,
  url_externa VARCHAR(1000) NULL,
  nombre_original VARCHAR(255) NOT NULL,
  tipo_mime VARCHAR(120) NOT NULL,
  tamano_bytes BIGINT UNSIGNED NULL,
  hash_sha256 BINARY(32) NULL,
  texto_accesibilidad VARCHAR(500) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK ((clave_almacenamiento IS NOT NULL) <> (url_externa IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_actividad (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  tipo ENUM('LECCION','RETO','CUESTIONARIO','TARJETAS_MEMORIA','INFOGRAFIA') NOT NULL,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  archivado_en DATETIME(6) NULL,
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_version_actividad (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_version INT UNSIGNED NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  instrucciones TEXT NULL,
  idioma VARCHAR(12) NOT NULL DEFAULT 'es-PE',
  dificultad TINYINT UNSIGNED NULL,
  minutos_estimados SMALLINT UNSIGNED NULL,
  estado ENUM('BORRADOR','PUBLICADO','RETIRADO') NOT NULL DEFAULT 'BORRADOR',
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  publicado_en DATETIME(6) NULL,
  UNIQUE KEY uq_actividad_version (actividad_id, numero_version),
  UNIQUE KEY uq_version_actividad_id (id, actividad_id),
  FOREIGN KEY (actividad_id) REFERENCES aprendizaje_actividad(id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (dificultad IS NULL OR dificultad BETWEEN 1 AND 5),
  CHECK (minutos_estimados IS NULL OR minutos_estimados > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_bloque_contenido (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_secuencia SMALLINT UNSIGNED NOT NULL,
  tipo ENUM('TEXTO','IMAGEN','AUDIO','VIDEO','DESTACADO','ADJUNTO') NOT NULL,
  texto_cuerpo TEXT NULL,
  archivo_multimedia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  texto_accesibilidad VARCHAR(500) NULL,
  UNIQUE KEY uq_bloque_secuencia (version_actividad_id, numero_secuencia),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (archivo_multimedia_id) REFERENCES recurso_archivo_multimedia(id),
  CHECK (texto_cuerpo IS NOT NULL OR archivo_multimedia_id IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_actividad_unidad (
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (version_actividad_id, unidad_id),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  propietario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  codigo VARCHAR(40) NULL UNIQUE,
  titulo VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  archivado_en DATETIME(6) NULL,
  FULLTEXT KEY ft_ruta_busqueda (titulo, descripcion),
  FOREIGN KEY (propietario_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_version_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  version_catalogo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  area_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_version INT UNSIGNED NOT NULL,
  estado ENUM('BORRADOR','PUBLICADO','PAUSADO','RETIRADO') NOT NULL DEFAULT 'BORRADOR',
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  publicado_en DATETIME(6) NULL,
  UNIQUE KEY uq_ruta_version (ruta_id, numero_version),
  UNIQUE KEY uq_version_ruta_catalogo (id, version_catalogo_id, grado_id, area_id),
  FOREIGN KEY (ruta_id) REFERENCES aprendizaje_ruta(id),
  FOREIGN KEY (version_catalogo_id) REFERENCES curriculo_version_catalogo(id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id),
  FOREIGN KEY (area_id, version_catalogo_id) REFERENCES curriculo_area(id, version_catalogo_id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_nodo_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  nodo_prerrequisito_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  numero_secuencia SMALLINT UNSIGNED NOT NULL,
  mapa_x DECIMAL(5,2) NULL,
  mapa_y DECIMAL(5,2) NULL,
  es_opcional BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uq_nodo_orden (version_ruta_id, numero_secuencia),
  UNIQUE KEY uq_nodo_version_ruta (version_ruta_id, id),
  KEY ix_nodo_actividad (version_actividad_id),
  FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id),
  FOREIGN KEY (version_ruta_id, nodo_prerrequisito_id) REFERENCES aprendizaje_nodo_ruta(version_ruta_id, id),
  CHECK ((mapa_x IS NULL AND mapa_y IS NULL) OR (mapa_x BETWEEN 0 AND 100 AND mapa_y BETWEEN 0 AND 100)),
  CHECK (nodo_prerrequisito_id IS NULL OR nodo_prerrequisito_id <> id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_codigo_acceso_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  hash_codigo BINARY(32) NOT NULL UNIQUE,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  valido_desde DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  valido_hasta DATETIME(6) NULL,
  maximo_usos INT UNSIGNED NULL,
  revocado_en DATETIME(6) NULL,
  FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (valido_hasta IS NULL OR valido_hasta > valido_desde),
  CHECK (maximo_usos IS NULL OR maximo_usos > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_inscripcion_ruta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo_acceso_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  estado ENUM('ACTIVO','COMPLETADO','RETIRADO','SUSPENDIDO') NOT NULL DEFAULT 'ACTIVO',
  ultimo_nodo_visitado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  inscrito_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completado_en DATETIME(6) NULL,
  UNIQUE KEY uq_ruta_estudiante (version_ruta_id, estudiante_id),
  UNIQUE KEY uq_inscripcion_version (id, version_ruta_id),
  FOREIGN KEY (version_ruta_id) REFERENCES aprendizaje_version_ruta(id),
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (codigo_acceso_id) REFERENCES aprendizaje_codigo_acceso_ruta(id),
  FOREIGN KEY (version_ruta_id, ultimo_nodo_visitado_id) REFERENCES aprendizaje_nodo_ruta(version_ruta_id, id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_progreso_nodo (
  inscripcion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  version_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  nodo_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  estado ENUM('BLOQUEADO','DISPONIBLE','EN_CURSO','COMPLETADO') NOT NULL DEFAULT 'BLOQUEADO',
  desbloqueado_en DATETIME(6) NULL,
  primer_acceso_en DATETIME(6) NULL,
  completado_en DATETIME(6) NULL,
  PRIMARY KEY (inscripcion_id, nodo_ruta_id),
  FOREIGN KEY (inscripcion_id, version_ruta_id) REFERENCES aprendizaje_inscripcion_ruta(id, version_ruta_id),
  FOREIGN KEY (version_ruta_id, nodo_ruta_id) REFERENCES aprendizaje_nodo_ruta(version_ruta_id, id),
  CHECK ((estado = 'BLOQUEADO') = (desbloqueado_en IS NULL)),
  CHECK ((estado = 'COMPLETADO') = (completado_en IS NOT NULL))
) ENGINE=InnoDB;

-- Identidad, permisos, sesiones y recuperacion de cuenta (historias 41-44, 58, 60).
CREATE TABLE IF NOT EXISTS identidad_rol (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL,
  descripcion VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_permiso (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(80) NOT NULL UNIQUE,
  descripcion VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_usuario_rol (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  rol_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  asignado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  asignado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (usuario_id, rol_id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (rol_id) REFERENCES identidad_rol(id),
  FOREIGN KEY (asignado_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_rol_permiso (
  rol_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  permiso_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (rol_id, permiso_id),
  FOREIGN KEY (rol_id) REFERENCES identidad_rol(id),
  FOREIGN KEY (permiso_id) REFERENCES identidad_permiso(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_sesion_usuario (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  token_hash BINARY(32) NOT NULL UNIQUE,
  revocada_en DATETIME(6) NULL,
  usuario_activo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin
    GENERATED ALWAYS AS (CASE WHEN revocada_en IS NULL THEN usuario_id ELSE NULL END) STORED,
  cliente ENUM('WEB_ADMIN','WEB_USUARIO','MOVIL') NOT NULL,
  creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actividad_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expira_en DATETIME(6) NOT NULL,
  UNIQUE KEY uq_sesion_usuario_activa (usuario_activo_id),
  KEY ix_sesion_usuario (usuario_id, expira_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (expira_en > creada_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_dispositivo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  plataforma ENUM('ANDROID','IOS','WEB') NOT NULL,
  hash_instalacion BINARY(32) NOT NULL,
  version_aplicacion VARCHAR(40) NULL,
  registrado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ultimo_acceso_en DATETIME(6) NULL,
  revocado_en DATETIME(6) NULL,
  UNIQUE KEY uq_dispositivo_usuario_instalacion (usuario_id, hash_instalacion),
  UNIQUE KEY uq_dispositivo_id_usuario (id, usuario_id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_intento_acceso (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  identificador_hash BINARY(32) NOT NULL,
  resultado ENUM('CORRECTO','INCORRECTO','BLOQUEADO') NOT NULL,
  origen_hash BINARY(32) NULL,
  ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY ix_intento_identificador_fecha (identificador_hash, ocurrido_en),
  KEY ix_intento_usuario_fecha (usuario_id, ocurrido_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_factor_mfa (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tipo ENUM('TOTP','CORREO') NOT NULL,
  secreto_cifrado VARBINARY(1024) NULL,
  referencia_clave VARCHAR(255) NULL,
  destino_enmascarado VARCHAR(120) NULL,
  verificado_en DATETIME(6) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  desactivado_en DATETIME(6) NULL,
  KEY ix_mfa_usuario (usuario_id, desactivado_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK ((tipo = 'TOTP' AND secreto_cifrado IS NOT NULL AND referencia_clave IS NOT NULL)
      OR (tipo = 'CORREO' AND destino_enmascarado IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_codigo_recuperacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  factor_mfa_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo_hash BINARY(32) NOT NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  usado_en DATETIME(6) NULL,
  UNIQUE KEY uq_codigo_recuperacion (factor_mfa_id, codigo_hash),
  FOREIGN KEY (factor_mfa_id) REFERENCES identidad_factor_mfa(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS identidad_token_restablecer_clave (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  token_hash BINARY(32) NOT NULL UNIQUE,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  expira_en DATETIME(6) NOT NULL,
  usado_en DATETIME(6) NULL,
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (expira_en > creado_en)
) ENGINE=InnoDB;

-- Instituciones, aulas, tutores legales y periodos escolares (historias 9-20, 25, 49-52).
CREATE TABLE IF NOT EXISTS escuela_institucion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(40) NULL UNIQUE,
  nombre VARCHAR(180) NOT NULL,
  nivel ENUM('PUBLICA','PRIVADA','OTRA') NOT NULL DEFAULT 'PUBLICA',
  ubigeo CHAR(6) NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_anio_escolar (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  institucion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  anio SMALLINT UNSIGNED NOT NULL,
  comienza DATE NULL,
  termina DATE NULL,
  estado ENUM('PLANIFICADO','ACTIVO','CERRADO') NOT NULL DEFAULT 'PLANIFICADO',
  UNIQUE KEY uq_anio_institucion (id, institucion_id),
  UNIQUE KEY uq_anio_escolar (institucion_id, anio),
  FOREIGN KEY (institucion_id) REFERENCES escuela_institucion(id),
  CHECK (termina IS NULL OR comienza IS NULL OR termina >= comienza)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_aula (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  institucion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  anio_escolar_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  docente_responsable_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_aula_anio_codigo (anio_escolar_id, codigo),
  UNIQUE KEY uq_aula_id_institucion (id, institucion_id),
  FOREIGN KEY (anio_escolar_id, institucion_id) REFERENCES escuela_anio_escolar(id, institucion_id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id),
  FOREIGN KEY (docente_responsable_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_membresia_aula (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  rol ENUM('DOCENTE','ESTUDIANTE','ASISTENTE') NOT NULL,
  estado ENUM('INVITADO','ACTIVO','RETIRADO') NOT NULL DEFAULT 'ACTIVO',
  ingreso_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  retiro_en DATETIME(6) NULL,
  UNIQUE KEY uq_membresia_aula_usuario (aula_id, usuario_id),
  UNIQUE KEY uq_membresia_id_aula (id, aula_id),
  FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK ((estado = 'RETIRADO') = (retiro_en IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_consentimiento_tutor (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tutor_usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  relacion VARCHAR(40) NULL,
  correo_tutor_cifrado VARBINARY(512) NULL,
  referencia_clave VARCHAR(255) NULL,
  proposito ENUM('CUENTA','TUTORIA_IA','NOTIFICACIONES','DATOS_ANALITICOS') NOT NULL,
  version_aviso VARCHAR(40) NOT NULL,
  metodo_verificacion ENUM('CODIGO_CORREO','REGISTRO_ESCOLAR','OTRO') NULL,
  evidencia_referencia VARCHAR(255) NULL,
  estado ENUM('PENDIENTE','OTORGADO','REVOCADO') NOT NULL DEFAULT 'PENDIENTE',
  registrado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  revocado_en DATETIME(6) NULL,
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (tutor_usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (tutor_usuario_id IS NOT NULL OR correo_tutor_cifrado IS NOT NULL),
  CHECK ((correo_tutor_cifrado IS NULL) = (referencia_clave IS NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_periodo_academico (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  anio_escolar_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tipo ENUM('BIMESTRE','TRIMESTRE','SEMESTRE','PERSONALIZADO') NOT NULL,
  numero TINYINT UNSIGNED NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  comienza DATE NOT NULL,
  termina DATE NOT NULL,
  UNIQUE KEY uq_periodo_anio_numero (anio_escolar_id, tipo, numero),
  FOREIGN KEY (anio_escolar_id) REFERENCES escuela_anio_escolar(id),
  CHECK (termina >= comienza)
) ENGINE=InnoDB;

-- Metas curriculares explicitas y estandares de referencia (historias 4, 49-52).
CREATE TABLE IF NOT EXISTS curriculo_meta_aprendizaje (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  competencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  codigo VARCHAR(60) NOT NULL,
  descripcion TEXT NOT NULL,
  UNIQUE KEY uq_meta_competencia_grado_codigo (competencia_id, grado_id, codigo),
  FOREIGN KEY (competencia_id) REFERENCES curriculo_competencia(id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_unidad_meta (
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  meta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (unidad_id, meta_id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id),
  FOREIGN KEY (meta_id) REFERENCES curriculo_meta_aprendizaje(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_estandar (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  competencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  codigo VARCHAR(60) NOT NULL,
  descripcion TEXT NOT NULL,
  UNIQUE KEY uq_estandar_competencia_codigo (competencia_id, codigo),
  FOREIGN KEY (competencia_id) REFERENCES curriculo_competencia(id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS curriculo_unidad_estandar (
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  estandar_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (unidad_id, estandar_id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id),
  FOREIGN KEY (estandar_id) REFERENCES curriculo_estandar(id)
) ENGINE=InnoDB;

-- Actividad de estudio, calendario, tareas, retos y repasos (historias 13-24, 33-40).
CREATE TABLE IF NOT EXISTS aprendizaje_sesion_estudio (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  inscripcion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  nodo_ruta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  inicio_en DATETIME(6) NOT NULL,
  fin_en DATETIME(6) NULL,
  origen ENUM('WEB','MOVIL') NOT NULL,
  cliente_evento_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  UNIQUE KEY uq_sesion_cliente_evento (usuario_id, cliente_evento_id),
  KEY ix_sesion_usuario_inicio (usuario_id, inicio_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (inscripcion_id) REFERENCES aprendizaje_inscripcion_ruta(id),
  FOREIGN KEY (nodo_ruta_id) REFERENCES aprendizaje_nodo_ruta(id),
  CHECK (fin_en IS NULL OR fin_en >= inicio_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS escuela_evento_calendario (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  descripcion TEXT NULL,
  tipo ENUM('CLASE','EVALUACION','ENTREGA','RECORDATORIO','OTRO') NOT NULL,
  inicia_en DATETIME(6) NOT NULL,
  termina_en DATETIME(6) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (termina_en IS NULL OR termina_en >= inicia_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_tarea (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  actividad_version_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  asignada_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  titulo VARCHAR(180) NOT NULL,
  instrucciones TEXT NULL,
  creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (actividad_version_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (asignada_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_entrega_tarea (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  tarea_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  asignada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  vence_en DATETIME(6) NULL,
  estado ENUM('PENDIENTE','EN_CURSO','ENTREGADA','REVISADA','ANULADA') NOT NULL DEFAULT 'PENDIENTE',
  FOREIGN KEY (tarea_id) REFERENCES aprendizaje_tarea(id),
  FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  UNIQUE KEY uq_entrega_tarea_estudiante (tarea_id, estudiante_id),
  UNIQUE KEY uq_entrega_id_estudiante (id, estudiante_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_reto_diario (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  fecha DATE NOT NULL,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  grado_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  estado ENUM('BORRADOR','PUBLICADO','RETIRADO') NOT NULL DEFAULT 'BORRADOR',
  UNIQUE KEY uq_reto_dia_grado (fecha, grado_id),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (grado_id) REFERENCES curriculo_grado(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_tarjeta_memoria (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_secuencia SMALLINT UNSIGNED NOT NULL,
  frente TEXT NOT NULL,
  reverso TEXT NOT NULL,
  UNIQUE KEY uq_tarjeta_actividad_orden (version_actividad_id, numero_secuencia),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_repaso_tarjeta (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tarjeta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  proximo_repaso_en DATETIME(6) NOT NULL,
  intervalo_dias SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  factor_facilidad DECIMAL(4,2) NOT NULL DEFAULT 2.50,
  repasada_en DATETIME(6) NULL,
  PRIMARY KEY (usuario_id, tarjeta_id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (tarjeta_id) REFERENCES aprendizaje_tarjeta_memoria(id),
  CHECK (factor_facilidad >= 1.30)
) ENGINE=InnoDB;

-- Evaluaciones configurables y respuestas normalizadas (historias 17-24, 33-36).
CREATE TABLE IF NOT EXISTS evaluacion_pregunta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_secuencia SMALLINT UNSIGNED NOT NULL,
  tipo ENUM('OPCION_UNICA','OPCION_MULTIPLE','VERDADERO_FALSO','TEXTO_CORTO') NOT NULL,
  enunciado TEXT NOT NULL,
  explicacion TEXT NULL,
  puntos DECIMAL(7,2) NOT NULL DEFAULT 1,
  UNIQUE KEY uq_pregunta_actividad_orden (version_actividad_id, numero_secuencia),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  CHECK (puntos >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluacion_opcion_respuesta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  pregunta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  numero_secuencia SMALLINT UNSIGNED NOT NULL,
  texto TEXT NOT NULL,
  es_correcta BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uq_opcion_pregunta_orden (pregunta_id, numero_secuencia),
  UNIQUE KEY uq_opcion_id_pregunta (id, pregunta_id),
  FOREIGN KEY (pregunta_id) REFERENCES evaluacion_pregunta(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluacion_intento (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  version_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  inscripcion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  entrega_tarea_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  numero_intento SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  iniciado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  finalizado_en DATETIME(6) NULL,
  puntaje DECIMAL(9,2) NULL,
  estado ENUM('EN_CURSO','COMPLETADO','ABANDONADO') NOT NULL DEFAULT 'EN_CURSO',
  cliente_evento_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  UNIQUE KEY uq_intento_cliente (estudiante_id, cliente_evento_id),
  KEY ix_intento_estudiante_fecha (estudiante_id, iniciado_en),
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (version_actividad_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (inscripcion_id) REFERENCES aprendizaje_inscripcion_ruta(id),
  FOREIGN KEY (entrega_tarea_id) REFERENCES aprendizaje_entrega_tarea(id),
  CHECK (puntaje IS NULL OR puntaje >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluacion_respuesta (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  intento_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  pregunta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  texto_respuesta TEXT NULL,
  es_correcta BOOLEAN NULL,
  puntaje_obtenido DECIMAL(9,2) NULL,
  respondida_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_respuesta_intento_pregunta (intento_id, pregunta_id),
  UNIQUE KEY uq_respuesta_id_pregunta (id, pregunta_id),
  FOREIGN KEY (intento_id) REFERENCES evaluacion_intento(id),
  FOREIGN KEY (pregunta_id) REFERENCES evaluacion_pregunta(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS evaluacion_respuesta_opcion (
  respuesta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  pregunta_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  opcion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (respuesta_id, opcion_id),
  FOREIGN KEY (respuesta_id, pregunta_id) REFERENCES evaluacion_respuesta(id, pregunta_id),
  FOREIGN KEY (opcion_id, pregunta_id) REFERENCES evaluacion_opcion_respuesta(id, pregunta_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aprendizaje_recomendacion_refuerzo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  estudiante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  unidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  intento_origen_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  actividad_sugerida_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  actividad_aprobada_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  entrega_tarea_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  motivo_codigo VARCHAR(80) NOT NULL,
  estado ENUM('PENDIENTE','APROBADA','AJUSTADA','RECHAZADA','ASIGNADA') NOT NULL DEFAULT 'PENDIENTE',
  decidida_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  decidida_en DATETIME(6) NULL,
  FOREIGN KEY (estudiante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (unidad_id) REFERENCES curriculo_unidad(id),
  FOREIGN KEY (intento_origen_id) REFERENCES evaluacion_intento(id),
  FOREIGN KEY (actividad_sugerida_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (actividad_aprobada_id) REFERENCES aprendizaje_version_actividad(id),
  FOREIGN KEY (entrega_tarea_id) REFERENCES aprendizaje_entrega_tarea(id),
  FOREIGN KEY (decidida_por) REFERENCES identidad_cuenta_usuario(id),
  KEY ix_recomendacion_estudiante_estado (estudiante_id, estado, creada_en)
) ENGINE=InnoDB;

-- Preferencias y entregas de notificaciones (historias 25-28).
CREATE TABLE IF NOT EXISTS notificacion_preferencia (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  no_molestar_desde TIME NULL,
  no_molestar_hasta TIME NULL,
  actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK ((no_molestar_desde IS NULL) = (no_molestar_hasta IS NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacion_preferencia_categoria (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  categoria ENUM('TAREA','RECORDATORIO','PROGRESO','SISTEMA','IA') NOT NULL,
  habilitada BOOLEAN NOT NULL DEFAULT TRUE,
  canal ENUM('INTERNA','CORREO','PUSH') NOT NULL DEFAULT 'INTERNA',
  PRIMARY KEY (usuario_id, categoria, canal),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacion_programacion (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  categoria ENUM('TAREA','RECORDATORIO','PROGRESO','SISTEMA','IA') NOT NULL,
  dia_semana TINYINT UNSIGNED NOT NULL,
  hora_local TIME NOT NULL,
  habilitada BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (usuario_id, categoria, dia_semana),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (dia_semana BETWEEN 1 AND 7)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacion_dispositivo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  dispositivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  token_cifrado VARBINARY(2048) NOT NULL,
  referencia_clave VARCHAR(255) NOT NULL,
  token_hash BINARY(32) NOT NULL UNIQUE,
  registrado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ultimo_uso_en DATETIME(6) NULL,
  revocado_en DATETIME(6) NULL,
  FOREIGN KEY (dispositivo_id) REFERENCES identidad_dispositivo(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacion_mensaje (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  categoria ENUM('TAREA','RECORDATORIO','PROGRESO','SISTEMA','IA') NOT NULL,
  titulo VARCHAR(160) NOT NULL,
  cuerpo VARCHAR(500) NOT NULL,
  destino_ruta VARCHAR(500) NULL,
  creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  leida_en DATETIME(6) NULL,
  expira_en DATETIME(6) NULL,
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  KEY ix_notificacion_usuario_fecha (usuario_id, creada_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notificacion_entrega (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  mensaje_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  dispositivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  canal ENUM('INTERNA','CORREO','PUSH') NOT NULL,
  estado ENUM('PENDIENTE','ENVIADA','ENTREGADA','FALLIDA','OMITIDA') NOT NULL DEFAULT 'PENDIENTE',
  proveedor_id VARCHAR(180) NULL,
  intentos TINYINT UNSIGNED NOT NULL DEFAULT 0,
  programada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completada_en DATETIME(6) NULL,
  FOREIGN KEY (mensaje_id) REFERENCES notificacion_mensaje(id),
  FOREIGN KEY (dispositivo_id) REFERENCES notificacion_dispositivo(id),
  KEY ix_entrega_estado_fecha (estado, programada_en)
) ENGINE=InnoDB;

-- Progreso gamificado: libro de movimientos, insignias, metas e inventario (historias 29-36).
CREATE TABLE IF NOT EXISTS gamificacion_meta_diaria (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  fecha_local DATE NOT NULL,
  tipo ENUM('MINUTOS','ACTIVIDADES','EXPERIENCIA') NOT NULL,
  objetivo DECIMAL(9,2) NOT NULL,
  actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (usuario_id, fecha_local, tipo),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (objetivo > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gamificacion_movimiento_experiencia (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  cantidad INT NOT NULL,
  motivo ENUM('ACTIVIDAD','RETO','INSIGNIA','AJUSTE','REVERSO') NOT NULL,
  referencia_tipo VARCHAR(40) NULL,
  referencia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  evento_idempotencia CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_xp_idempotencia (usuario_id, evento_idempotencia),
  KEY ix_xp_usuario_fecha (usuario_id, ocurrido_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  CHECK (cantidad <> 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gamificacion_insignia (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  nombre VARCHAR(120) NOT NULL,
  descripcion VARCHAR(255) NOT NULL,
  icono_archivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  criterio JSON NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (icono_archivo_id) REFERENCES recurso_archivo_multimedia(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gamificacion_insignia_usuario (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  insignia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  obtenida_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (usuario_id, insignia_id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (insignia_id) REFERENCES gamificacion_insignia(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gamificacion_accesorio (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  nombre VARCHAR(120) NOT NULL,
  tipo ENUM('SOMBRERO','VESTIMENTA','FONDO','MARCO','ANIMACION') NOT NULL,
  archivo_multimedia_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  costo_experiencia INT UNSIGNED NOT NULL DEFAULT 0,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  FOREIGN KEY (archivo_multimedia_id) REFERENCES recurso_archivo_multimedia(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS gamificacion_inventario_usuario (
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  accesorio_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  obtenido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  equipado BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (usuario_id, accesorio_id),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (accesorio_id) REFERENCES gamificacion_accesorio(id)
) ENGINE=InnoDB;

-- IA: politicas por version/aula, conversaciones restringidas, moderacion y generacion (13-16, 21-24, 37-40, 53-56).
CREATE TABLE IF NOT EXISTS ia_politica (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL,
  numero_version INT UNSIGNED NOT NULL,
  nivel_ayuda ENUM('PISTAS','GUIADO','EXPLICACION') NOT NULL DEFAULT 'GUIADO',
  instrucciones_sistema TEXT NOT NULL,
  edad_minima TINYINT UNSIGNED NULL,
  edad_maxima TINYINT UNSIGNED NULL,
  retencion_dias SMALLINT UNSIGNED NOT NULL DEFAULT 30,
  estado ENUM('BORRADOR','ACTIVA','RETIRADA') NOT NULL DEFAULT 'BORRADOR',
  creado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_politica_version (codigo, numero_version),
  FOREIGN KEY (creado_por) REFERENCES identidad_cuenta_usuario(id),
  CHECK (edad_maxima IS NULL OR edad_minima IS NULL OR edad_maxima >= edad_minima)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_politica_aula (
  aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  politica_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  habilitada BOOLEAN NOT NULL DEFAULT FALSE,
  actualizada_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  actualizada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
  FOREIGN KEY (politica_id) REFERENCES ia_politica(id),
  FOREIGN KEY (actualizada_por) REFERENCES identidad_cuenta_usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_configuracion_proveedor (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  modelo VARCHAR(120) NOT NULL,
  secreto_referencia VARCHAR(255) NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT FALSE,
  parametros_no_secretos JSON NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_conversacion_tutor (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  aula_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  politica_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  actividad_version_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  iniciada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  cerrada_en DATETIME(6) NULL,
  eliminar_despues DATETIME(6) NULL,
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (aula_id) REFERENCES escuela_aula(id),
  FOREIGN KEY (politica_id) REFERENCES ia_politica(id),
  FOREIGN KEY (actividad_version_id) REFERENCES aprendizaje_version_actividad(id),
  KEY ix_conversacion_usuario_fecha (usuario_id, iniciada_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_mensaje_tutor (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  conversacion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  secuencia INT UNSIGNED NOT NULL,
  emisor ENUM('ESTUDIANTE','TUTOR_IA','SISTEMA') NOT NULL,
  contenido TEXT NOT NULL,
  modelo VARCHAR(120) NULL,
  creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_mensaje_conversacion_orden (conversacion_id, secuencia),
  FOREIGN KEY (conversacion_id) REFERENCES ia_conversacion_tutor(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_regla_moderacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  categoria ENUM('SEGURIDAD','PRIVACIDAD','LENGUAJE','FUERA_DE_TEMA','JAILBREAK') NOT NULL,
  patron_hash BINARY(32) NULL,
  patron_cifrado VARBINARY(2048) NULL,
  referencia_clave VARCHAR(255) NULL,
  accion ENUM('PERMITIR','ADVERTIR','BLOQUEAR','DERIVAR') NOT NULL,
  severidad TINYINT UNSIGNED NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (severidad BETWEEN 1 AND 5),
  CHECK ((patron_cifrado IS NULL) = (referencia_clave IS NULL))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_evento_moderacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  mensaje_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  regla_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  categoria ENUM('SEGURIDAD','PRIVACIDAD','LENGUAJE','FUERA_DE_TEMA','JAILBREAK') NOT NULL,
  accion ENUM('PERMITIR','ADVERTIR','BLOQUEAR','DERIVAR') NOT NULL,
  puntaje DECIMAL(5,4) NULL,
  requiere_revision BOOLEAN NOT NULL DEFAULT FALSE,
  revisado_por CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  revisado_en DATETIME(6) NULL,
  ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY (mensaje_id) REFERENCES ia_mensaje_tutor(id),
  FOREIGN KEY (regla_id) REFERENCES ia_regla_moderacion(id),
  FOREIGN KEY (revisado_por) REFERENCES identidad_cuenta_usuario(id),
  KEY ix_moderacion_revision (requiere_revision, revisado_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ia_tarea_generacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  solicitante_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  proveedor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  tipo ENUM('CUESTIONARIO','TARJETAS','INFOGRAFIA','AUDIO','EJEMPLOS','REMEDIACION') NOT NULL,
  estado ENUM('PENDIENTE','PROCESANDO','COMPLETADA','FALLIDA','CANCELADA') NOT NULL DEFAULT 'PENDIENTE',
  entrada_referencia VARCHAR(500) NULL,
  resultado_actividad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  resultado_archivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  error_codigo VARCHAR(80) NULL,
  solicitada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completada_en DATETIME(6) NULL,
  FOREIGN KEY (solicitante_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (proveedor_id) REFERENCES ia_configuracion_proveedor(id),
  FOREIGN KEY (resultado_actividad_id) REFERENCES aprendizaje_actividad(id),
  FOREIGN KEY (resultado_archivo_id) REFERENCES recurso_archivo_multimedia(id),
  KEY ix_generacion_estado_fecha (estado, solicitada_en)
) ENGINE=InnoDB;

-- Sincronizacion movil, respaldo y auditoria acotada. Metricas de infraestructura permanecen externas (45-48, 57-59).
CREATE TABLE IF NOT EXISTS operaciones_evento_sincronizacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  dispositivo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  evento_cliente_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  tipo_evento VARCHAR(80) NOT NULL,
  estado ENUM('RECIBIDO','APLICADO','DUPLICADO','RECHAZADO','ERROR') NOT NULL,
  codigo_resultado VARCHAR(80) NULL,
  recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sync_usuario_evento (usuario_id, evento_cliente_id),
  KEY ix_sync_estado_fecha (estado, recibido_en),
  FOREIGN KEY (usuario_id) REFERENCES identidad_cuenta_usuario(id),
  FOREIGN KEY (dispositivo_id) REFERENCES identidad_dispositivo(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operaciones_respaldo (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  tipo ENUM('COMPLETO','INCREMENTAL','BINLOG') NOT NULL,
  iniciado_en DATETIME(6) NOT NULL,
  finalizado_en DATETIME(6) NULL,
  estado ENUM('EN_CURSO','CORRECTO','FALLIDO','VERIFICADO') NOT NULL,
  ubicacion_referencia VARCHAR(500) NULL,
  checksum_sha256 BINARY(32) NULL,
  tamano_bytes BIGINT UNSIGNED NULL,
  version_esquema VARCHAR(60) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operaciones_prueba_recuperacion (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  respaldo_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  iniciada_en DATETIME(6) NOT NULL,
  finalizada_en DATETIME(6) NULL,
  resultado ENUM('EXITOSA','FALLIDA','PARCIAL') NOT NULL,
  duracion_segundos INT UNSIGNED NULL,
  informe_referencia VARCHAR(500) NULL,
  ejecutada_por VARCHAR(120) NOT NULL,
  FOREIGN KEY (respaldo_id) REFERENCES operaciones_respaldo(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operaciones_incidente (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  servicio VARCHAR(80) NOT NULL,
  categoria ENUM('DISPONIBILIDAD','RENDIMIENTO','SINCRONIZACION','IA','BASE_DATOS','OTRO') NOT NULL,
  severidad ENUM('BAJA','MEDIA','ALTA','CRITICA') NOT NULL,
  estado ENUM('ABIERTO','EN_ATENCION','RESUELTO','CERRADO') NOT NULL DEFAULT 'ABIERTO',
  resumen VARCHAR(500) NOT NULL,
  referencia_externa VARCHAR(255) NULL,
  iniciado_en DATETIME(6) NOT NULL,
  resuelto_en DATETIME(6) NULL,
  CHECK (resuelto_en IS NULL OR resuelto_en >= iniciado_en)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operaciones_evento_auditoria (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  actor_usuario_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  dominio VARCHAR(60) NOT NULL,
  accion VARCHAR(80) NOT NULL,
  entidad_tipo VARCHAR(80) NOT NULL,
  entidad_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  resultado ENUM('CORRECTO','RECHAZADO','ERROR') NOT NULL,
  ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  correlacion_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
  FOREIGN KEY (actor_usuario_id) REFERENCES identidad_cuenta_usuario(id),
  KEY ix_auditoria_entidad (entidad_tipo, entidad_id, ocurrido_en),
  KEY ix_auditoria_actor (actor_usuario_id, ocurrido_en)
) ENGINE=InnoDB;

-- Los seis grados son catálogo estable del producto; las áreas y unidades oficiales
-- deben cargarse desde una versión del Currículo Nacional revisada por el equipo.
INSERT IGNORE INTO curriculo_grado (id, numero_grado, nombre) VALUES
 ('00000000-0000-4000-8000-000000000001', 1, '1.° de primaria'),
 ('00000000-0000-4000-8000-000000000002', 2, '2.° de primaria'),
 ('00000000-0000-4000-8000-000000000003', 3, '3.° de primaria'),
 ('00000000-0000-4000-8000-000000000004', 4, '4.° de primaria'),
 ('00000000-0000-4000-8000-000000000005', 5, '5.° de primaria'),
 ('00000000-0000-4000-8000-000000000006', 6, '6.° de primaria');
