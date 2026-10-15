package pe.rupi.api.teacher;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TeacherRouteRepository {

    private final JdbcTemplate jdbc;

    public TeacherRouteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TeacherDtos.TeacherRouteSummary> findTeacherRoutes(String teacherId) {
        return jdbc.query("""
                SELECT r.id AS route_id,
                       vr.id AS version_route_id,
                       r.codigo AS code,
                       r.titulo,
                       r.descripcion,
                       g.numero_grado,
                       g.nombre AS grade_name,
                       a.codigo AS area_code,
                       a.nombre AS area_name,
                       vr.estado,
                       (SELECT COUNT(*) FROM aprendizaje_nodo_ruta n WHERE n.version_ruta_id = vr.id) AS total_nodes,
                       vr.creado_en,
                       vr.publicado_en
                FROM aprendizaje_ruta r
                JOIN aprendizaje_version_ruta vr ON vr.ruta_id = r.id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                WHERE r.propietario_id = ? AND r.archivado_en IS NULL
                ORDER BY vr.creado_en DESC
                """, (rs, rowNum) -> mapRouteSummary(rs), teacherId);
    }

    public Optional<TeacherDtos.TeacherRouteDetail> findTeacherRouteDetail(String teacherId, String routeId) {
        List<TeacherDtos.TeacherRouteDetail> list = jdbc.query("""
                SELECT r.id AS route_id,
                       vr.id AS version_route_id,
                       r.codigo AS code,
                       r.titulo,
                       r.descripcion,
                       g.numero_grado,
                       g.nombre AS grade_name,
                       a.codigo AS area_code,
                       a.nombre AS area_name,
                       vr.estado,
                       vr.creado_en,
                       vr.publicado_en
                FROM aprendizaje_ruta r
                JOIN aprendizaje_version_ruta vr ON vr.ruta_id = r.id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                WHERE r.id = ? AND r.propietario_id = ? AND r.archivado_en IS NULL
                """, (rs, rowNum) -> {
            String rId = rs.getString("route_id");
            String vId = rs.getString("version_route_id");
            String code = rs.getString("code");
            String title = rs.getString("titulo");
            String desc = rs.getString("descripcion");
            int gNum = rs.getInt("numero_grado");
            String gName = rs.getString("grade_name");
            String aCode = rs.getString("area_code");
            String aName = rs.getString("area_name");
            String state = rs.getString("estado");
            String created = formatTimestamp(rs.getTimestamp("creado_en"));
            String published = formatTimestamp(rs.getTimestamp("publicado_en"));

            List<TeacherDtos.TeacherNodeDto> nodes = findNodesByVersion(vId);

            return new TeacherDtos.TeacherRouteDetail(
                    rId, vId, code, title, desc,
                    new TeacherDtos.GradeInfo(gNum, gName),
                    new TeacherDtos.AreaInfo(aCode, aName),
                    state, nodes, created, published
            );
        }, routeId, teacherId);

        return list.stream().findFirst();
    }

    public List<TeacherDtos.TeacherNodeDto> findNodesByVersion(String versionRouteId) {
        List<TeacherDtos.TeacherNodeDto> nodes = jdbc.query("""
                SELECT n.id,
                       n.numero_secuencia,
                       va.titulo,
                       va.instrucciones,
                       a.tipo AS activity_type,
                       va.minutos_estimados,
                       n.mapa_x,
                       n.mapa_y,
                       n.disponible_desde,
                       n.bloqueado_desde,
                       (SELECT COALESCE(m.url_externa, m.clave_almacenamiento, '')
                        FROM aprendizaje_bloque_contenido b
                        LEFT JOIN recurso_archivo_multimedia m ON m.id = b.archivo_multimedia_id
                        WHERE b.version_actividad_id = va.id AND (b.tipo = 'VIDEO' OR b.archivo_multimedia_id IS NOT NULL)
                        ORDER BY b.numero_secuencia ASC LIMIT 1) AS video_url
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                JOIN aprendizaje_actividad a ON a.id = va.actividad_id
                WHERE n.version_ruta_id = ?
                ORDER BY n.numero_secuencia ASC
                """, (rs, rowNum) -> new TeacherDtos.TeacherNodeDto(
                rs.getString("id"),
                rs.getInt("numero_secuencia"),
                rs.getString("titulo"),
                rs.getString("instrucciones"),
                rs.getString("activity_type"),
                rs.getObject("minutos_estimados", Integer.class),
                rs.getObject("mapa_x", Double.class),
                rs.getObject("mapa_y", Double.class),
                formatTimestamp(rs.getTimestamp("disponible_desde")),
                formatTimestamp(rs.getTimestamp("bloqueado_desde")),
                rs.getString("video_url"),
                new ArrayList<>()
        ), versionRouteId);

        if (nodes.isEmpty()) {
            return nodes;
        }

        // Cargar recursos adjuntos vinculados a los nodos
        for (var node : nodes) {
            List<TeacherDtos.TeacherResourceDto> resources = jdbc.query("""
                    SELECT id, tipo, titulo, url_recurso, archivo_multimedia_id, creado_en
                    FROM aprendizaje_nodo_recurso
                    WHERE nodo_id = ?
                    ORDER BY creado_en ASC
                    """, (rs, rowNum) -> new TeacherDtos.TeacherResourceDto(
                    rs.getString("id"),
                    rs.getString("tipo"),
                    rs.getString("titulo"),
                    rs.getString("url_recurso"),
                    rs.getString("archivo_multimedia_id"),
                    formatTimestamp(rs.getTimestamp("creado_en"))
            ), node.id());
            node.resources().addAll(resources);
        }

        return nodes;
    }

    @Transactional
    public TeacherDtos.CreateRouteResponse createRoute(String teacherId, TeacherDtos.CreateRouteRequest req) {
        String routeId = UUID.randomUUID().toString();
        String versionRouteId = UUID.randomUUID().toString();
        String routeCode = "RO-DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String catalogId = getActiveCatalogId();
        String gradeId = getGradeIdByNumber(req.gradeNumber());
        String areaId = getAreaIdByCode(req.areaCode(), catalogId);

        jdbc.update("""
                INSERT INTO aprendizaje_ruta (id, propietario_id, codigo, titulo, descripcion, creado_en, actualizado_en)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, routeId, teacherId, routeCode, req.title().trim(), req.description() != null ? req.description().trim() : null);

        jdbc.update("""
                INSERT INTO aprendizaje_version_ruta (
                    id, ruta_id, version_catalogo_id, grado_id, area_id, numero_version, estado, creado_por, creado_en
                ) VALUES (?, ?, ?, ?, ?, 1, 'BORRADOR', ?, UTC_TIMESTAMP(6))
                """, versionRouteId, routeId, catalogId, gradeId, areaId, teacherId);

        // Registrar código de acceso para que los estudiantes puedan ingresar directamente
        jdbc.update("""
                INSERT INTO aprendizaje_codigo_acceso_ruta (
                    id, version_ruta_id, hash_codigo, creado_por, valido_desde, valido_hasta, maximo_usos
                ) VALUES (?, ?, UNHEX(SHA2(?, 256)), ?, UTC_TIMESTAMP(6), DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 2 YEAR), 200)
                """, UUID.randomUUID().toString(), versionRouteId, routeCode, teacherId);

        return new TeacherDtos.CreateRouteResponse(
                routeId,
                versionRouteId,
                routeCode,
                req.title().trim(),
                "BORRADOR",
                "Roadmap pedagógico creado exitosamente con código: " + routeCode
        );
    }

    /**
     * Última versión de una ruta del docente. Con queryForObject, una ruta ajena o inexistente lanzaba
     * EmptyResultDataAccessException y la API respondía 500 en vez de 404.
     */
    private String latestOwnedVersionId(String teacherId, String routeId) {
        return jdbc.query("""
                SELECT vr.id
                FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE r.id = ? AND r.propietario_id = ? AND r.archivado_en IS NULL
                ORDER BY vr.numero_version DESC LIMIT 1
                """, (rs, rowNum) -> rs.getString("id"), routeId, teacherId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ruta no encontrada o no pertenece al docente."));
    }

    @Transactional
    public String addNodeToRoute(String teacherId, String routeId, TeacherDtos.CreateNodeRequest req) {
        String versionRouteId = latestOwnedVersionId(teacherId, routeId);

        // Una secuencia ya ocupada chocaba con la clave única uq_nodo_orden (500): la parada se agrega al final.
        int sequence = req.sequence() != null && req.sequence() > 0 && !isSequenceTaken(versionRouteId, req.sequence())
                ? req.sequence()
                : getNextNodeSequence(versionRouteId);

        String activityId = UUID.randomUUID().toString();
        String versionActividadId = UUID.randomUUID().toString();
        String nodeId = UUID.randomUUID().toString();
        String activityType = req.activityType() != null && !req.activityType().isBlank() ? req.activityType().toUpperCase() : "LECCION";
        int estimatedMinutes = req.estimatedMinutes() != null && req.estimatedMinutes() > 0 ? req.estimatedMinutes() : 10;

        // 1. Actividad y versión de actividad
        jdbc.update("""
                INSERT INTO aprendizaje_actividad (id, tipo, creado_por, creado_en)
                VALUES (?, ?, ?, UTC_TIMESTAMP(6))
                """, activityId, activityType, teacherId);

        jdbc.update("""
                INSERT INTO aprendizaje_version_actividad (
                    id, actividad_id, numero_version, titulo, instrucciones, minutos_estimados, estado, creado_en
                ) VALUES (?, ?, 1, ?, ?, ?, 'PUBLICADO', UTC_TIMESTAMP(6))
                """, versionActividadId, activityId, req.title().trim(), req.instructions() != null ? req.instructions().trim() : "Explora esta parada y realiza las actividades didácticas con tu profesor.", estimatedMinutes);

        // 2. Bloque introductorio de contenido
        String blockId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo)
                VALUES (?, ?, 1, 'DESTACADO', ?)
                """, blockId, versionActividadId, "Bienvenido a esta lección preparada especialmente por tu profesor.");

        // Coordenadas armónicas en zigzag si no se especifican
        Double mapX = req.mapX() != null ? req.mapX() : calculateDefaultMapX(sequence);
        Double mapY = req.mapY() != null ? req.mapY() : calculateDefaultMapY(sequence);

        // 3. Nodo de ruta
        jdbc.update("""
                INSERT INTO aprendizaje_nodo_ruta (
                    id, version_ruta_id, version_actividad_id, numero_secuencia, mapa_x, mapa_y, es_opcional
                ) VALUES (?, ?, ?, ?, ?, ?, FALSE)
                """, nodeId, versionRouteId, versionActividadId, sequence, mapX, mapY);

        createProgressForEnrolledStudents(versionRouteId, nodeId, sequence);

        return nodeId;
    }

    /**
     * El progreso por parada se crea al inscribirse. Una parada agregada después (la ruta puede estar publicada)
     * no tenía fila para los alumnos ya inscritos, y al completar la anterior el desbloqueo no encontraba qué
     * actualizar: la parada quedaba bloqueada para siempre. Aquí nace DISPONIBLE si la parada anterior ya está
     * completada (o si es la primera), y BLOQUEADA en otro caso.
     */
    private void createProgressForEnrolledStudents(String versionRouteId, String nodeId, int sequence) {
        jdbc.update("""
                INSERT IGNORE INTO aprendizaje_progreso_nodo
                    (inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en)
                SELECT i.id, i.version_ruta_id, ?,
                       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN 'DISPONIBLE' ELSE 'BLOQUEADO' END,
                       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN UTC_TIMESTAMP(6) ELSE NULL END
                FROM aprendizaje_inscripcion_ruta i
                LEFT JOIN aprendizaje_nodo_ruta prev
                       ON prev.version_ruta_id = i.version_ruta_id
                      AND prev.numero_secuencia = (
                          SELECT MAX(x.numero_secuencia) FROM aprendizaje_nodo_ruta x
                          WHERE x.version_ruta_id = i.version_ruta_id AND x.numero_secuencia < ?)
                LEFT JOIN aprendizaje_progreso_nodo pp
                       ON pp.inscripcion_id = i.id AND pp.nodo_ruta_id = prev.id
                WHERE i.version_ruta_id = ?
                """, nodeId, sequence, versionRouteId);
    }

    private boolean isSequenceTaken(String versionRouteId, int sequence) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ? AND numero_secuencia = ?
                """, Integer.class, versionRouteId, sequence);
        return count != null && count > 0;
    }

    @Transactional
    public String attachResourceToNode(String teacherId, String nodeId, TeacherDtos.AttachResourceRequest req) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_ruta vr ON vr.id = n.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE n.id = ? AND r.propietario_id = ?
                """, Integer.class, nodeId, teacherId);

        if (count == null || count == 0) {
            throw new IllegalArgumentException("La parada no pertenece a un roadmap de su autoría.");
        }

        String resourceId = UUID.randomUUID().toString();
        String type = req.type() != null && !req.type().isBlank() ? req.type().trim().toUpperCase(Locale.ROOT) : "DOCUMENTO";
        String multimediaId = req.multimediaId() != null && !req.multimediaId().isBlank() ? req.multimediaId().trim() : null;

        if (multimediaId != null) {
            // Solo materiales subidos por el mismo docente: antes un id ajeno se enlazaba y uno inexistente daba 500.
            Integer owned = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM recurso_archivo_multimedia WHERE id = ? AND creado_por = ?
                    """, Integer.class, multimediaId, teacherId);
            if (owned == null || owned == 0) {
                throw new IllegalArgumentException("El material indicado no existe o no fue subido por usted.");
            }
        } else {
            // Un enlace externo no tenía registro multimedia: el bloque del alumno quedaba sin URL y sin botón para
            // abrirlo (la dirección solo aparecía dentro del texto). Se registra como recurso con url_externa.
            multimediaId = UUID.randomUUID().toString();
            jdbc.update("""
                    INSERT INTO recurso_archivo_multimedia (id, creado_por, url_externa, nombre_original, tipo_mime, creado_en)
                    VALUES (?, ?, ?, ?, 'text/uri-list', UTC_TIMESTAMP(6))
                    """, multimediaId, teacherId, req.url().trim(), req.title().trim());
        }

        jdbc.update("""
                INSERT INTO aprendizaje_nodo_recurso (
                    id, nodo_id, tipo, titulo, url_recurso, archivo_multimedia_id, creado_en
                ) VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, resourceId, nodeId, type, req.title().trim(), req.url().trim(), multimediaId);

        // Enlazar también un bloque de contenido en la versión de actividad de la parada
        String versionActividadId = jdbc.queryForObject("""
                SELECT version_actividad_id FROM aprendizaje_nodo_ruta WHERE id = ?
                """, String.class, nodeId);

        if (versionActividadId != null) {
            int nextSeq = jdbc.queryForObject("""
                    SELECT COALESCE(MAX(numero_secuencia), 1) + 1
                    FROM aprendizaje_bloque_contenido
                    WHERE version_actividad_id = ?
                    """, Integer.class, versionActividadId);

            String blockId = UUID.randomUUID().toString();
            String linkText = "Material adjunto por tu profesor: " + req.title().trim() + " (" + req.url().trim() + ")";

            jdbc.update("""
                    INSERT INTO aprendizaje_bloque_contenido (
                        id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo, archivo_multimedia_id
                    ) VALUES (?, ?, ?, 'ADJUNTO', ?, ?)
                    """, blockId, versionActividadId, nextSeq, linkText, multimediaId);
        }

        return resourceId;
    }

    @Transactional
    public TeacherDtos.PublishRouteResponse publishRoute(String teacherId, String routeId) {
        String versionRouteId = latestOwnedVersionId(teacherId, routeId);

        Integer nodeCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ?
                """, Integer.class, versionRouteId);

        if (nodeCount == null || nodeCount == 0) {
            throw new IllegalStateException("Para publicar un roadmap, debe contener al menos una parada pedagógica.");
        }

        // Reanudar una ruta pausada usa este mismo paso; publicado_en conserva la fecha de la primera publicación.
        jdbc.update("""
                UPDATE aprendizaje_version_ruta
                SET estado = 'PUBLICADO', publicado_en = COALESCE(publicado_en, UTC_TIMESTAMP(6))
                WHERE id = ?
                """, versionRouteId);

        return new TeacherDtos.PublishRouteResponse(
                routeId,
                versionRouteId,
                "PUBLICADO",
                formatTimestamp(jdbc.queryForObject(
                        "SELECT publicado_en FROM aprendizaje_version_ruta WHERE id = ?", java.sql.Timestamp.class, versionRouteId)),
                "¡El roadmap ha sido publicado exitosamente! Ahora está disponible para sus estudiantes."
        );
    }

    /**
     * Historia 8: pausa una ruta publicada. Todas las consultas del alumno exigen estado PUBLICADO, así que
     * deja de aparecer en sus rutas, en la búsqueda y en las descargas, y abrir o completar paradas responde 404.
     */
    @Transactional
    public TeacherDtos.PublishRouteResponse pauseRoute(String teacherId, String routeId) {
        String versionRouteId = latestOwnedVersionId(teacherId, routeId);
        int updated = jdbc.update("""
                UPDATE aprendizaje_version_ruta SET estado = 'PAUSADO'
                WHERE id = ? AND estado = 'PUBLICADO'
                """, versionRouteId);
        if (updated == 0) {
            throw new IllegalStateException("Solo se puede pausar un roadmap publicado.");
        }
        return new TeacherDtos.PublishRouteResponse(
                routeId,
                versionRouteId,
                "PAUSADO",
                formatTimestamp(jdbc.queryForObject(
                        "SELECT publicado_en FROM aprendizaje_version_ruta WHERE id = ?", java.sql.Timestamp.class, versionRouteId)),
                "El roadmap quedó en pausa: tus estudiantes no podrán verlo hasta que lo vuelvas a publicar."
        );
    }

    @Transactional
    public void reorderNodes(String teacherId, String routeId, List<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de paradas a reordenar no puede estar vacía.");
        }

        // 1. Obtener la última versión de la ruta del profesor
        List<String> versionRouteIds = jdbc.query("""
                SELECT vr.id
                FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE r.id = ? AND r.propietario_id = ?
                ORDER BY vr.numero_version DESC LIMIT 1
                """, (rs, rowNum) -> rs.getString("id"), routeId, teacherId);

        if (versionRouteIds.isEmpty()) {
            throw new IllegalArgumentException("El roadmap no existe o no pertenece a su autoría.");
        }
        String versionRouteId = versionRouteIds.get(0);

        // 2. Obtener los IDs actuales de los nodos de esta versión
        List<String> existingNodeIds = jdbc.query("""
                SELECT id FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ?
                """, (rs, rowNum) -> rs.getString("id"), versionRouteId);

        if (existingNodeIds.size() != nodeIds.size()) {
            throw new IllegalArgumentException("La cantidad de nodos enviados (" + nodeIds.size()
                    + ") no coincide con las paradas del roadmap (" + existingNodeIds.size() + ").");
        }

        Set<String> existingSet = new HashSet<>(existingNodeIds);
        for (String id : nodeIds) {
            if (!existingSet.contains(id)) {
                throw new IllegalArgumentException("El nodo " + id + " no pertenece a este roadmap.");
            }
        }

        // 3. Desplazar temporalmente numero_secuencia (+1000) para evitar colisión con UNIQUE KEY uq_nodo_orden (version_ruta_id, numero_secuencia)
        jdbc.update("""
                UPDATE aprendizaje_nodo_ruta
                SET numero_secuencia = numero_secuencia + 1000
                WHERE version_ruta_id = ?
                """, versionRouteId);

        // 4. Asignar la nueva secuencia y coordenadas actualizadas del mapa interactivo
        for (int i = 0; i < nodeIds.size(); i++) {
            int newSequence = i + 1;
            String nodeId = nodeIds.get(i);
            Double newMapX = calculateDefaultMapX(newSequence);
            Double newMapY = calculateDefaultMapY(newSequence);

            jdbc.update("""
                    UPDATE aprendizaje_nodo_ruta
                    SET numero_secuencia = ?, mapa_x = ?, mapa_y = ?
                    WHERE id = ? AND version_ruta_id = ?
                    """, newSequence, newMapX, newMapY, nodeId, versionRouteId);
        }
    }

    private String getActiveCatalogId() {
        try {
            return jdbc.queryForObject("""
                    SELECT id FROM curriculo_version_catalogo WHERE estado = 'ACTIVO' LIMIT 1
                    """, String.class);
        } catch (EmptyResultDataAccessException e) {
            return "c0000000-0000-4000-8000-000000000001";
        }
    }

    private String getGradeIdByNumber(int gradeNumber) {
        try {
            return jdbc.queryForObject("""
                    SELECT id FROM curriculo_grado WHERE numero_grado = ? LIMIT 1
                    """, String.class, gradeNumber);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Grado escolar no encontrado: " + gradeNumber);
        }
    }

    private String getAreaIdByCode(String areaCode, String catalogId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id FROM curriculo_area
                    WHERE (LOWER(codigo) = LOWER(?) OR LOWER(nombre) LIKE LOWER(?))
                      AND version_catalogo_id = ?
                    LIMIT 1
                    """, String.class, areaCode, "%" + areaCode + "%", catalogId);
        } catch (EmptyResultDataAccessException e) {
            // Antes se tomaba un área cualquiera: la ruta quedaba en una materia que el docente no eligió.
            throw new IllegalArgumentException("Área curricular no encontrada: " + areaCode);
        }
    }

    private int getNextNodeSequence(String versionRouteId) {
        Integer max = jdbc.queryForObject("""
                SELECT MAX(numero_secuencia) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ?
                """, Integer.class, versionRouteId);
        return max != null ? max + 1 : 1;
    }

    private static Double calculateDefaultMapX(int sequence) {
        double[] xPattern = { 50.0, 75.0, 25.0, 80.0, 20.0, 60.0 };
        return xPattern[(sequence - 1) % xPattern.length];
    }

    private static Double calculateDefaultMapY(int sequence) {
        return Math.min(95.0, 15.0 + (sequence - 1) * 15.0);
    }

    private static TeacherDtos.TeacherRouteSummary mapRouteSummary(ResultSet rs) throws SQLException {
        return new TeacherDtos.TeacherRouteSummary(
                rs.getString("route_id"),
                rs.getString("version_route_id"),
                rs.getString("code"),
                rs.getString("titulo"),
                rs.getString("descripcion"),
                rs.getInt("numero_grado"),
                rs.getString("grade_name"),
                rs.getString("area_code"),
                rs.getString("area_name"),
                rs.getString("estado"),
                rs.getInt("total_nodes"),
                formatTimestamp(rs.getTimestamp("creado_en")),
                formatTimestamp(rs.getTimestamp("publicado_en"))
        );
    }

    public Optional<TeacherDtos.TeacherNodeDetailDto> findNodeDetail(String teacherId, String routeId, String nodeId) {
        List<TeacherDtos.TeacherNodeDetailDto> list = jdbc.query("""
                SELECT n.id,
                       n.numero_secuencia,
                       va.id AS version_actividad_id,
                       va.titulo,
                       va.instrucciones,
                       a.tipo AS activity_type,
                       va.minutos_estimados,
                       n.mapa_x,
                       n.mapa_y,
                       n.disponible_desde,
                       n.bloqueado_desde,
                       (SELECT COALESCE(m.url_externa, m.clave_almacenamiento, '')
                        FROM aprendizaje_bloque_contenido b
                        LEFT JOIN recurso_archivo_multimedia m ON m.id = b.archivo_multimedia_id
                        WHERE b.version_actividad_id = va.id AND (b.tipo = 'VIDEO' OR b.archivo_multimedia_id IS NOT NULL)
                        ORDER BY b.numero_secuencia ASC LIMIT 1) AS video_url
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_ruta vr ON vr.id = n.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                JOIN aprendizaje_actividad a ON a.id = va.actividad_id
                WHERE n.id = ? AND r.id = ? AND r.propietario_id = ? AND r.archivado_en IS NULL
                """, (rs, rowNum) -> {
            String id = rs.getString("id");
            int seq = rs.getInt("numero_secuencia");
            String versionActividadId = rs.getString("version_actividad_id");
            String title = rs.getString("titulo");
            String instructions = rs.getString("instrucciones");
            String activityType = rs.getString("activity_type");
            Integer minutes = rs.getObject("minutos_estimados", Integer.class);
            Double mapX = rs.getObject("mapa_x", Double.class);
            Double mapY = rs.getObject("mapa_y", Double.class);
            String availableFrom = formatTimestamp(rs.getTimestamp("disponible_desde"));
            String lockedFrom = formatTimestamp(rs.getTimestamp("bloqueado_desde"));
            String videoUrl = rs.getString("video_url");

            List<TeacherDtos.TeacherResourceDto> resources = jdbc.query("""
                    SELECT id, tipo, titulo, url_recurso, archivo_multimedia_id, creado_en
                    FROM aprendizaje_nodo_recurso
                    WHERE nodo_id = ?
                    ORDER BY creado_en ASC
                    """, (rRes, rNum) -> new TeacherDtos.TeacherResourceDto(
                    rRes.getString("id"),
                    rRes.getString("tipo"),
                    rRes.getString("titulo"),
                    rRes.getString("url_recurso"),
                    rRes.getString("archivo_multimedia_id"),
                    formatTimestamp(rRes.getTimestamp("creado_en"))
            ), id);

            List<TeacherDtos.TeacherQuestionDto> questions = findQuestionsWithAnswers(versionActividadId);

            return new TeacherDtos.TeacherNodeDetailDto(
                    id, seq, title, instructions, activityType, minutes, mapX, mapY,
                    availableFrom, lockedFrom, videoUrl, questions, resources
            );
        }, nodeId, routeId, teacherId);

        return list.stream().findFirst();
    }

    public List<TeacherDtos.TeacherQuestionDto> findQuestionsWithAnswers(String versionActividadId) {
        List<TeacherDtos.TeacherQuestionDto> questions = jdbc.query("""
                SELECT id, numero_secuencia, tipo, enunciado, explicacion, puntos
                FROM evaluacion_pregunta
                WHERE version_actividad_id = ?
                ORDER BY numero_secuencia ASC
                """, (rs, rowNum) -> new TeacherDtos.TeacherQuestionDto(
                rs.getString("id"),
                rs.getInt("numero_secuencia"),
                rs.getString("tipo"),
                rs.getString("enunciado"),
                rs.getString("explicacion"),
                rs.getDouble("puntos"),
                new ArrayList<>()
        ), versionActividadId);

        for (var q : questions) {
            List<TeacherDtos.TeacherOptionDto> options = jdbc.query("""
                    SELECT id, numero_secuencia, texto, es_correcta
                    FROM evaluacion_opcion_respuesta
                    WHERE pregunta_id = ?
                    ORDER BY numero_secuencia ASC
                    """, (rs, rowNum) -> new TeacherDtos.TeacherOptionDto(
                    rs.getString("id"),
                    rs.getInt("numero_secuencia"),
                    rs.getString("texto"),
                    rs.getBoolean("es_correcta")
            ), q.id());
            q.options().addAll(options);
        }

        return questions;
    }

    @Transactional
    public void updateNode(String teacherId, String routeId, String nodeId, TeacherDtos.UpdateNodeRequest req) {
        String versionActividadId = jdbc.query("""
                SELECT n.version_actividad_id
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_ruta vr ON vr.id = n.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE n.id = ? AND r.id = ? AND r.propietario_id = ? AND r.archivado_en IS NULL
                """, (rs, rowNum) -> rs.getString("version_actividad_id"), nodeId, routeId, teacherId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Parada no encontrada o no pertenece al docente."));

        if (req.title() != null && !req.title().isBlank()) {
            jdbc.update("""
                    UPDATE aprendizaje_version_actividad
                    SET titulo = ?,
                        instrucciones = ?,
                        minutos_estimados = COALESCE(?, minutos_estimados)
                    WHERE id = ?
                    """, req.title().trim(),
                    req.instructions() != null ? req.instructions().trim() : null,
                    req.estimatedMinutes(), versionActividadId);
        } else if (req.instructions() != null || req.estimatedMinutes() != null) {
            jdbc.update("""
                    UPDATE aprendizaje_version_actividad
                    SET instrucciones = COALESCE(?, instrucciones),
                        minutos_estimados = COALESCE(?, minutos_estimados)
                    WHERE id = ?
                    """, req.instructions() != null ? req.instructions().trim() : null,
                    req.estimatedMinutes(), versionActividadId);
        }

        if (req.activityType() != null && !req.activityType().isBlank()) {
            jdbc.update("""
                    UPDATE aprendizaje_actividad a
                    JOIN aprendizaje_version_actividad va ON va.actividad_id = a.id
                    SET a.tipo = ?
                    WHERE va.id = ?
                    """, req.activityType().trim().toUpperCase(Locale.ROOT), versionActividadId);
        }

        java.sql.Timestamp availableFrom = parseTimestamp(req.availableFrom());
        java.sql.Timestamp lockedFrom = parseTimestamp(req.lockedFrom());

        jdbc.update("""
                UPDATE aprendizaje_nodo_ruta
                SET disponible_desde = ?,
                    bloqueado_desde = ?
                WHERE id = ?
                """, availableFrom, lockedFrom, nodeId);

        if (req.videoUrl() != null) {
            String trimmedUrl = req.videoUrl().trim();
            if (!trimmedUrl.isEmpty()) {
                List<String> videoBlocks = jdbc.query("""
                        SELECT b.id FROM aprendizaje_bloque_contenido b
                        WHERE b.version_actividad_id = ? AND b.tipo = 'VIDEO'
                        ORDER BY b.numero_secuencia ASC LIMIT 1
                        """, (rs, rowNum) -> rs.getString("id"), versionActividadId);

                if (!videoBlocks.isEmpty()) {
                    String blockId = videoBlocks.getFirst();
                    List<String> multiList = jdbc.query("""
                            SELECT archivo_multimedia_id FROM aprendizaje_bloque_contenido WHERE id = ?
                            """, (rs, rowNum) -> rs.getString("archivo_multimedia_id"), blockId);
                    String multimediaId = !multiList.isEmpty() ? multiList.getFirst() : null;

                    if (multimediaId != null) {
                        jdbc.update("""
                                UPDATE recurso_archivo_multimedia
                                SET url_externa = ?, nombre_original = ?
                                WHERE id = ?
                                """, trimmedUrl, req.title() != null ? req.title().trim() : "Video de lección", multimediaId);
                    } else {
                        String newMultiId = UUID.randomUUID().toString();
                        jdbc.update("""
                                INSERT INTO recurso_archivo_multimedia (id, creado_por, url_externa, nombre_original, tipo_mime, creado_en)
                                VALUES (?, ?, ?, ?, 'text/uri-list', UTC_TIMESTAMP(6))
                                """, newMultiId, teacherId, trimmedUrl, req.title() != null ? req.title().trim() : "Video de lección");
                        jdbc.update("""
                                UPDATE aprendizaje_bloque_contenido SET archivo_multimedia_id = ? WHERE id = ?
                                """, newMultiId, blockId);
                    }
                    jdbc.update("""
                            UPDATE aprendizaje_bloque_contenido SET texto_cuerpo = ? WHERE id = ?
                            """, req.title() != null ? req.title().trim() : "Video de lección", blockId);
                } else {
                    String newMultiId = UUID.randomUUID().toString();
                    jdbc.update("""
                            INSERT INTO recurso_archivo_multimedia (id, creado_por, url_externa, nombre_original, tipo_mime, creado_en)
                            VALUES (?, ?, ?, ?, 'text/uri-list', UTC_TIMESTAMP(6))
                            """, newMultiId, teacherId, trimmedUrl, req.title() != null ? req.title().trim() : "Video de lección");
                    String newBlockId = UUID.randomUUID().toString();
                    jdbc.update("""
                            INSERT INTO aprendizaje_bloque_contenido (id, version_actividad_id, numero_secuencia, tipo, texto_cuerpo, archivo_multimedia_id)
                            VALUES (?, ?, 1, 'VIDEO', ?, ?)
                            """, newBlockId, versionActividadId, req.title() != null ? req.title().trim() : "Video de lección", newMultiId);
                }
            } else {
                jdbc.update("""
                        DELETE FROM aprendizaje_bloque_contenido WHERE version_actividad_id = ? AND tipo = 'VIDEO'
                        """, versionActividadId);
            }
        }

        if (req.questions() != null) {
            updateQuestionsForActivity(versionActividadId, req.questions());
        }
    }

    private void updateQuestionsForActivity(String versionActividadId, List<TeacherDtos.TeacherQuestionDto> questions) {
        List<String> oldQuestionIds = jdbc.query("""
                SELECT id FROM evaluacion_pregunta WHERE version_actividad_id = ?
                """, (rs, rowNum) -> rs.getString("id"), versionActividadId);

        for (String qId : oldQuestionIds) {
            jdbc.update("DELETE FROM evaluacion_respuesta_opcion WHERE pregunta_id = ?", qId);
            jdbc.update("DELETE FROM evaluacion_respuesta WHERE pregunta_id = ?", qId);
            jdbc.update("DELETE FROM evaluacion_opcion_respuesta WHERE pregunta_id = ?", qId);
        }
        jdbc.update("DELETE FROM evaluacion_pregunta WHERE version_actividad_id = ?", versionActividadId);

        int qSeq = 1;
        for (var q : questions) {
            String qId = q.id() != null && !q.id().isBlank() ? q.id() : UUID.randomUUID().toString();
            String statement = q.statement() != null ? q.statement().trim() : "Pregunta didáctica";
            String explanation = q.explanation() != null ? q.explanation().trim() : "¡Buen esfuerzo repasando los conceptos!";
            double points = q.points() != null && q.points() > 0 ? q.points() : 1.0;
            String qType = q.type() != null && !q.type().isBlank() ? q.type() : "OPCION_UNICA";

            jdbc.update("""
                    INSERT INTO evaluacion_pregunta (id, version_actividad_id, numero_secuencia, tipo, enunciado, explicacion, puntos)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, qId, versionActividadId, qSeq++, qType, statement, explanation, points);

            int optSeq = 1;
            boolean hasCorrect = false;
            if (q.options() != null) {
                for (var opt : q.options()) {
                    String optId = opt.id() != null && !opt.id().isBlank() ? opt.id() : UUID.randomUUID().toString();
                    String optText = opt.text() != null ? opt.text().trim() : "Opción " + optSeq;
                    boolean isCorrect = Boolean.TRUE.equals(opt.isCorrect());
                    if (isCorrect) hasCorrect = true;

                    jdbc.update("""
                            INSERT INTO evaluacion_opcion_respuesta (id, pregunta_id, numero_secuencia, texto, es_correcta)
                            VALUES (?, ?, ?, ?, ?)
                            """, optId, qId, optSeq++, optText, isCorrect);
                }
            }
            if (!hasCorrect && q.options() != null && !q.options().isEmpty()) {
                jdbc.update("""
                        UPDATE evaluacion_opcion_respuesta SET es_correcta = TRUE WHERE pregunta_id = ? AND numero_secuencia = 1
                        """, qId);
            }
        }
    }

    private static java.sql.Timestamp parseTimestamp(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        try {
            java.time.Instant instant = java.time.Instant.parse(isoDate);
            return java.sql.Timestamp.from(instant);
        } catch (Exception e1) {
            try {
                String cleaned = isoDate.replace("T", " ");
                if (cleaned.length() == 16) cleaned += ":00";
                return java.sql.Timestamp.valueOf(cleaned);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Transactional
    public void deleteNode(String teacherId, String routeId, String nodeId) {
        String versionRouteId = latestOwnedVersionId(teacherId, routeId);

        List<Map<String, Object>> nodeRows = jdbc.queryForList("""
                SELECT n.id, n.version_actividad_id, n.numero_secuencia
                FROM aprendizaje_nodo_ruta n
                WHERE n.id = ? AND n.version_ruta_id = ?
                """, nodeId, versionRouteId);

        if (nodeRows.isEmpty()) {
            throw new IllegalArgumentException("La parada didáctica no existe o no pertenece a este roadmap.");
        }

        String versionActividadId = (String) nodeRows.get(0).get("version_actividad_id");

        // 1. Limpiar progresos asociados a la parada
        jdbc.update("DELETE FROM aprendizaje_progreso_nodo WHERE nodo_ruta_id = ?", nodeId);

        // 2. Limpiar recursos adjuntos a la parada
        jdbc.update("DELETE FROM aprendizaje_nodo_recurso WHERE nodo_id = ?", nodeId);

        // 3. Limpiar preguntas de evaluación y bloques de contenido
        if (versionActividadId != null) {
            List<String> questionIds = jdbc.query("""
                    SELECT id FROM evaluacion_pregunta WHERE version_actividad_id = ?
                    """, (rs, rowNum) -> rs.getString("id"), versionActividadId);

            for (String qId : questionIds) {
                jdbc.update("DELETE FROM evaluacion_respuesta_opcion WHERE pregunta_id = ?", qId);
                jdbc.update("DELETE FROM evaluacion_respuesta WHERE pregunta_id = ?", qId);
                jdbc.update("DELETE FROM evaluacion_opcion_respuesta WHERE pregunta_id = ?", qId);
            }
            jdbc.update("DELETE FROM evaluacion_pregunta WHERE version_actividad_id = ?", versionActividadId);
            jdbc.update("DELETE FROM aprendizaje_bloque_contenido WHERE version_actividad_id = ?", versionActividadId);
        }

        // 4. Eliminar el nodo de la ruta
        jdbc.update("DELETE FROM aprendizaje_nodo_ruta WHERE id = ? AND version_ruta_id = ?", nodeId, versionRouteId);

        // 5. Eliminar la versión de actividad y actividad si no están en uso por otras paradas
        if (versionActividadId != null) {
            Integer usedCount = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_actividad_id = ?
                    """, Integer.class, versionActividadId);
            if (usedCount == null || usedCount == 0) {
                try {
                    String actId = jdbc.queryForObject("""
                            SELECT actividad_id FROM aprendizaje_version_actividad WHERE id = ?
                            """, String.class, versionActividadId);
                    jdbc.update("DELETE FROM aprendizaje_version_actividad WHERE id = ?", versionActividadId);
                    if (actId != null) {
                        jdbc.update("DELETE FROM aprendizaje_actividad WHERE id = ?", actId);
                    }
                } catch (EmptyResultDataAccessException ignored) {}
            }
        }

        // 6. Renumerar secuencias de los nodos restantes
        List<String> remainingNodes = jdbc.query("""
                SELECT id FROM aprendizaje_nodo_ruta
                WHERE version_ruta_id = ?
                ORDER BY numero_secuencia ASC
                """, (rs, rowNum) -> rs.getString("id"), versionRouteId);

        jdbc.update("UPDATE aprendizaje_nodo_ruta SET numero_secuencia = numero_secuencia + 1000 WHERE version_ruta_id = ?", versionRouteId);

        for (int i = 0; i < remainingNodes.size(); i++) {
            int newSeq = i + 1;
            Double newMapX = calculateDefaultMapX(newSeq);
            Double newMapY = calculateDefaultMapY(newSeq);
            jdbc.update("""
                    UPDATE aprendizaje_nodo_ruta
                    SET numero_secuencia = ?, mapa_x = ?, mapa_y = ?
                    WHERE id = ?
                    """, newSeq, newMapX, newMapY, remainingNodes.get(i));
        }
    }

    @Transactional
    public void deleteRoute(String teacherId, String routeId) {
        String versionRouteId = latestOwnedVersionId(teacherId, routeId);

        int updated = jdbc.update("""
                UPDATE aprendizaje_ruta
                SET archivado_en = UTC_TIMESTAMP(6)
                WHERE id = ? AND propietario_id = ? AND archivado_en IS NULL
                """, routeId, teacherId);

        if (updated == 0) {
            throw new IllegalArgumentException("El roadmap no existe o no pertenece a su autoría.");
        }

        jdbc.update("""
                UPDATE aprendizaje_version_ruta
                SET estado = 'RETIRADO'
                WHERE ruta_id = ?
                """, routeId);

        jdbc.update("""
                UPDATE aprendizaje_codigo_acceso_ruta
                SET revocado_en = UTC_TIMESTAMP(6)
                WHERE version_ruta_id = ?
                """, versionRouteId);
    }

    private static String formatTimestamp(java.sql.Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().toString();
    }
}
