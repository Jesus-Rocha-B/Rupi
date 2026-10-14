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
                WHERE r.propietario_id = ?
                ORDER BY vr.creado_en DESC
                """, (rs, rowNum) -> mapRouteSummary(rs), teacherId);
    }

    public Optional<TeacherDtos.TeacherRouteDetail> findTeacherRouteDetail(String teacherId, String routeId) {
        List<TeacherDtos.TeacherRouteDetail> list = jdbc.query("""
                SELECT r.id AS route_id,
                       vr.id AS version_route_id,
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
                WHERE r.id = ? AND r.propietario_id = ?
                """, (rs, rowNum) -> {
            String rId = rs.getString("route_id");
            String vId = rs.getString("version_route_id");
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
                    rId, vId, title, desc,
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
                       n.mapa_y
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

        return new TeacherDtos.CreateRouteResponse(
                routeId,
                versionRouteId,
                req.title().trim(),
                "BORRADOR",
                "Roadmap pedagógico creado exitosamente en modo borrador."
        );
    }

    @Transactional
    public String addNodeToRoute(String teacherId, String routeId, TeacherDtos.CreateNodeRequest req) {
        String versionRouteId = jdbc.queryForObject("""
                SELECT vr.id
                FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE r.id = ? AND r.propietario_id = ?
                ORDER BY vr.numero_version DESC LIMIT 1
                """, String.class, routeId, teacherId);

        if (versionRouteId == null) {
            throw new IllegalArgumentException("Ruta no encontrada o no pertenece al docente.");
        }

        int sequence = req.sequence() != null && req.sequence() > 0 ? req.sequence() : getNextNodeSequence(versionRouteId);

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

        return nodeId;
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
        String type = req.type() != null && !req.type().isBlank() ? req.type().toUpperCase() : "DOCUMENTO";

        jdbc.update("""
                INSERT INTO aprendizaje_nodo_recurso (
                    id, nodo_id, tipo, titulo, url_recurso, archivo_multimedia_id, creado_en
                ) VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, resourceId, nodeId, type, req.title().trim(), req.url().trim(),
                (req.multimediaId() != null && !req.multimediaId().isBlank()) ? req.multimediaId() : null);

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
                    """, blockId, versionActividadId, nextSeq, linkText,
                    (req.multimediaId() != null && !req.multimediaId().isBlank()) ? req.multimediaId() : null);
        }

        return resourceId;
    }

    @Transactional
    public TeacherDtos.PublishRouteResponse publishRoute(String teacherId, String routeId) {
        String versionRouteId = jdbc.queryForObject("""
                SELECT vr.id
                FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE r.id = ? AND r.propietario_id = ?
                ORDER BY vr.numero_version DESC LIMIT 1
                """, String.class, routeId, teacherId);

        if (versionRouteId == null) {
            throw new IllegalArgumentException("Ruta no encontrada o no pertenece al docente.");
        }

        Integer nodeCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = ?
                """, Integer.class, versionRouteId);

        if (nodeCount == null || nodeCount == 0) {
            throw new IllegalStateException("Para publicar un roadmap, debe contener al menos una parada pedagógica.");
        }

        jdbc.update("""
                UPDATE aprendizaje_version_ruta
                SET estado = 'PUBLICADO', publicado_en = UTC_TIMESTAMP(6)
                WHERE id = ?
                """, versionRouteId);

        return new TeacherDtos.PublishRouteResponse(
                routeId,
                versionRouteId,
                "PUBLICADO",
                formatTimestamp(new java.sql.Timestamp(System.currentTimeMillis())),
                "¡El roadmap ha sido publicado exitosamente! Ahora está disponible para sus estudiantes."
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
            try {
                return jdbc.queryForObject("""
                        SELECT id FROM curriculo_area LIMIT 1
                        """, String.class);
            } catch (EmptyResultDataAccessException ex) {
                throw new IllegalArgumentException("Área curricular no encontrada: " + areaCode);
            }
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

    private static String formatTimestamp(java.sql.Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().toString();
    }
}
