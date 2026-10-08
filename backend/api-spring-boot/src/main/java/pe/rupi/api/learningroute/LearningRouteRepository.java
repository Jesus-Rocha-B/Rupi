package pe.rupi.api.learningroute;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import pe.rupi.api.learningroute.LearningRouteDtos.Area;
import pe.rupi.api.learningroute.LearningRouteDtos.CulturalContext;
import pe.rupi.api.learningroute.LearningRouteDtos.CulturalFact;
import pe.rupi.api.learningroute.LearningRouteDtos.Curriculum;
import pe.rupi.api.learningroute.LearningRouteDtos.Enrollment;
import pe.rupi.api.learningroute.LearningRouteDtos.Grade;
import pe.rupi.api.learningroute.LearningRouteDtos.Position;
import pe.rupi.api.learningroute.LearningRouteDtos.Progress;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteNode;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteSummary;

@Repository
public class LearningRouteRepository {
    private static final String ENROLLMENT_FROM = """
            FROM aprendizaje_inscripcion_ruta i
            JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
            JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
            JOIN curriculo_grado g ON g.id = vr.grado_id
            JOIN curriculo_area a ON a.id = vr.area_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public LearningRouteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RouteSummary> findRoutesForStudent(UUID studentId) {
        String sql = """
                SELECT vr.id AS version_route_id, r.titulo AS route_title,
                       g.numero_grado, g.nombre AS grade_name,
                       a.codigo AS area_code, a.nombre AS area_name,
                       i.estado AS enrollment_state, i.ultima_visita_en,
                       COUNT(DISTINCT n.id) AS total_nodes,
                       COUNT(DISTINCT CASE WHEN p.estado = 'COMPLETADO' THEN n.id END) AS completed_nodes
                """ + ENROLLMENT_FROM + """
                LEFT JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.version_ruta_id = vr.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :studentId
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                  AND r.archivado_en IS NULL
                GROUP BY vr.id, r.titulo, g.numero_grado, g.nombre,
                         a.codigo, a.nombre, i.estado, i.inscrito_en, i.ultima_visita_en
                ORDER BY i.ultima_visita_en DESC, g.numero_grado, a.nombre, r.titulo, vr.id
                """;

        return jdbc.query(sql, new MapSqlParameterSource("studentId", studentId.toString()), (rs, rowNum) ->
                new RouteSummary(
                        uuid(rs, "version_route_id"),
                        rs.getString("route_title"),
                        new Grade(rs.getInt("numero_grado"), rs.getString("grade_name")),
                        new Area(rs.getString("area_code"), rs.getString("area_name")),
                        rs.getString("enrollment_state"),
                        rs.getLong("completed_nodes"),
                        rs.getLong("total_nodes"), rs.getString("ultima_visita_en")
                ));
    }

    public Optional<RouteHeader> findRouteForStudent(UUID studentId, UUID versionRouteId) {
        String sql = """
                SELECT vr.id AS version_route_id, r.id AS route_id, r.titulo AS route_title,
                       g.numero_grado, g.nombre AS grade_name,
                       a.codigo AS area_code, a.nombre AS area_name,
                       i.id AS enrollment_id, i.estado AS enrollment_state,
                       i.ultimo_nodo_visitado_id AS last_node_id
                """ + ENROLLMENT_FROM + """
                WHERE i.estudiante_id = :studentId
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                  AND r.archivado_en IS NULL
                  AND vr.id = :routeId
                """;

        var parameters = new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("routeId", versionRouteId.toString());

        return jdbc.query(sql, parameters, rs -> {
            if (!rs.next()) return Optional.empty();
            return Optional.of(new RouteHeader(
                    uuid(rs, "version_route_id"),
                    uuid(rs, "route_id"),
                    rs.getString("route_title"),
                    new Grade(rs.getInt("numero_grado"), rs.getString("grade_name")),
                    new Area(rs.getString("area_code"), rs.getString("area_name")),
                    new Enrollment(uuid(rs, "enrollment_id"), rs.getString("enrollment_state"),
                            nullableUuid(rs, "last_node_id"))
            ));
        });
    }

    public Progress findProgress(UUID enrollmentId, UUID versionRouteId) {
        String sql = """
                SELECT COUNT(DISTINCT n.id) AS total_nodes,
                       COUNT(DISTINCT CASE WHEN p.estado = 'COMPLETADO' THEN n.id END) AS completed_nodes
                FROM aprendizaje_nodo_ruta n
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = :enrollmentId
                      AND p.version_ruta_id = n.version_ruta_id
                      AND p.nodo_ruta_id = n.id
                WHERE n.version_ruta_id = :routeId
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("enrollmentId", enrollmentId.toString())
                .addValue("routeId", versionRouteId.toString());
        return jdbc.queryForObject(sql, parameters, (rs, rowNum) ->
                new Progress(rs.getLong("completed_nodes"), rs.getLong("total_nodes")));
    }

    public List<RouteNode> findNodes(UUID enrollmentId, UUID versionRouteId) {
        String sql = """
                SELECT n.id AS node_id, n.numero_secuencia, va.titulo AS activity_title,
                       act.tipo AS activity_type, va.minutos_estimados,
                       CASE WHEN va.estado <> 'PUBLICADO' OR act.archivado_en IS NOT NULL THEN 'BLOQUEADO' ELSE p.estado END AS progress_state,
                       n.es_opcional, n.mapa_x, n.mapa_y
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                JOIN aprendizaje_actividad act ON act.id = va.actividad_id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = :enrollmentId
                      AND p.version_ruta_id = n.version_ruta_id
                      AND p.nodo_ruta_id = n.id
                WHERE n.version_ruta_id = :routeId
                ORDER BY n.numero_secuencia
                """;
        var parameters = new MapSqlParameterSource()
                .addValue("enrollmentId", enrollmentId.toString())
                .addValue("routeId", versionRouteId.toString());
        return jdbc.query(sql, parameters, NODE_MAPPER);
    }

<<<<<<< HEAD
    public Optional<RouteCurriculum.Data> findCurriculum(UUID versionRouteId) {
        var parameters = new MapSqlParameterSource("routeId", versionRouteId.toString());
        var header = jdbc.query("""
                SELECT vr.version_catalogo_id, vr.grado_id, vr.area_id, vc.estado AS catalog_state
                FROM aprendizaje_version_ruta vr
                JOIN curriculo_version_catalogo vc ON vc.id = vr.version_catalogo_id
                WHERE vr.id = :routeId
                """, parameters, rs -> rs.next()
                ? new String[] {rs.getString("version_catalogo_id"), rs.getString("grado_id"),
                        rs.getString("area_id"), rs.getString("catalog_state")}
                : null);
        if (header == null) return Optional.empty();

        var nodeUnits = jdbc.query("""
                SELECT id, unidad_id FROM aprendizaje_nodo_ruta
                WHERE version_ruta_id = :routeId ORDER BY numero_secuencia
                """, parameters, (rs, rowNum) -> new RouteCurriculum.NodeUnit(uuid(rs, "id"), nullableUuid(rs, "unidad_id")));
        var units = jdbc.query("""
                SELECT DISTINCT u.id, u.codigo, u.titulo, u.numero_secuencia,
                       u.version_catalogo_id, u.grado_id, u.area_id
                FROM aprendizaje_nodo_ruta n
                JOIN curriculo_unidad u ON u.id = n.unidad_id
                WHERE n.version_ruta_id = :routeId
                """, parameters, (rs, rowNum) -> new RouteCurriculum.UnitRow(
                uuid(rs, "id"), rs.getString("codigo"), rs.getString("titulo"),
                (Integer) rs.getObject("numero_secuencia"), uuid(rs, "version_catalogo_id"),
                uuid(rs, "grado_id"), uuid(rs, "area_id")));
        var competencies = jdbc.query("""
                SELECT DISTINCT uc.unidad_id, c.codigo, c.nombre, ar.version_catalogo_id, ar.id AS area_id
                FROM aprendizaje_nodo_ruta n
                JOIN curriculo_unidad_competencia uc ON uc.unidad_id = n.unidad_id
                JOIN curriculo_competencia c ON c.id = uc.competencia_id
                JOIN curriculo_area ar ON ar.id = c.area_id
                WHERE n.version_ruta_id = :routeId
                """, parameters, (rs, rowNum) -> new RouteCurriculum.CompetencyRow(
                uuid(rs, "unidad_id"), rs.getString("codigo"), rs.getString("nombre"),
                uuid(rs, "version_catalogo_id"), uuid(rs, "area_id")));
        return Optional.of(new RouteCurriculum.Data(UUID.fromString(header[0]), header[3],
                UUID.fromString(header[1]), UUID.fromString(header[2]), nodeUnits, units, competencies));
=======
    public Optional<RouteCurriculum.Data> findCurriculumData(UUID versionRouteId) {
        String sqlHeader = """
                SELECT vr.version_catalogo_id AS catalogo_id, vc.estado AS catalogo_estado, vr.grado_id, vr.area_id
                FROM aprendizaje_version_ruta vr
                JOIN curriculo_version_catalogo vc ON vc.id = vr.version_catalogo_id
                WHERE vr.id = :routeId
                """;
        var params = new MapSqlParameterSource("routeId", versionRouteId.toString());
        return jdbc.query(sqlHeader, params, rs -> {
            if (!rs.next()) return Optional.empty();
            UUID catalogId = uuid(rs, "catalogo_id");
            String catalogState = rs.getString("catalogo_estado");
            UUID gradeId = uuid(rs, "grado_id");
            UUID areaId = uuid(rs, "area_id");

            String sqlNodes = """
                    SELECT n.id AS node_id, n.unidad_id
                    FROM aprendizaje_nodo_ruta n
                    WHERE n.version_ruta_id = :routeId AND n.unidad_id IS NOT NULL
                    """;
            List<RouteCurriculum.NodeUnit> nodeUnits = jdbc.query(sqlNodes, params, (r, i) ->
                    new RouteCurriculum.NodeUnit(uuid(r, "node_id"), uuid(r, "unidad_id")));

            String sqlUnits = """
                    SELECT u.id AS unit_id, u.codigo, u.titulo, u.numero_secuencia, u.version_catalogo_id AS catalogo_id, u.grado_id, u.area_id
                    FROM curriculo_unidad u
                    WHERE u.version_catalogo_id = :catalogId AND u.grado_id = :gradeId AND u.area_id = :areaId
                    ORDER BY u.numero_secuencia
                    """;
            var unitParams = new MapSqlParameterSource("catalogId", catalogId.toString())
                    .addValue("gradeId", gradeId.toString())
                    .addValue("areaId", areaId.toString());
            List<RouteCurriculum.UnitRow> units = jdbc.query(sqlUnits, unitParams, (r, i) ->
                    new RouteCurriculum.UnitRow(
                            uuid(r, "unit_id"),
                            r.getString("codigo"),
                            r.getString("titulo"),
                            r.getInt("numero_secuencia"),
                            uuid(r, "catalogo_id"),
                            uuid(r, "grado_id"),
                            uuid(r, "area_id")
                    ));

            String sqlCompetencies = """
                    SELECT uc.unidad_id, c.codigo, c.nombre, a.version_catalogo_id AS catalogo_id, c.area_id
                    FROM curriculo_unidad_competencia uc
                    JOIN curriculo_competencia c ON c.id = uc.competencia_id
                    JOIN curriculo_area a ON a.id = c.area_id
                    JOIN curriculo_unidad u ON u.id = uc.unidad_id
                    WHERE u.version_catalogo_id = :catalogId AND u.grado_id = :gradeId AND u.area_id = :areaId
                    """;
            List<RouteCurriculum.CompetencyRow> competencies = jdbc.query(sqlCompetencies, unitParams, (r, i) ->
                    new RouteCurriculum.CompetencyRow(
                            uuid(r, "unidad_id"),
                            r.getString("codigo"),
                            r.getString("nombre"),
                            uuid(r, "catalogo_id"),
                            uuid(r, "area_id")
                    ));

            return Optional.of(new RouteCurriculum.Data(catalogId, catalogState, gradeId, areaId, nodeUnits, units, competencies));
        });
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    }

    private static final RowMapper<RouteNode> NODE_MAPPER = (rs, rowNum) -> {
        BigDecimal x = rs.getBigDecimal("mapa_x");
        BigDecimal y = rs.getBigDecimal("mapa_y");
        Position position = x == null || y == null ? null : new Position(x, y);
        return new RouteNode(
                uuid(rs, "node_id"),
                rs.getInt("numero_secuencia"),
                rs.getString("activity_title"),
                rs.getString("activity_type"),
                (Integer) rs.getObject("minutos_estimados"),
                rs.getString("progress_state"),
                rs.getBoolean("es_opcional"),
                position
        );
    };

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return UUID.fromString(rs.getString(column));
    }

    private static UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    public Optional<CulturalContext> findCulturalContextForRoute(UUID routeId) {
        String sql = """
                SELECT id, codigo, ciudad, lugar, titulo, descripcion, mensaje_animo,
                       imagen_url, imagen_alt, credito_autor, credito_fuente,
                       credito_licencia, credito_url
                FROM curriculo_contexto_cultural
                WHERE ruta_id = :routeId
                """;
        var parameters = new MapSqlParameterSource("routeId", routeId.toString());
        return jdbc.query(sql, parameters, rs -> {
            if (!rs.next()) return Optional.empty();
            UUID contextId = uuid(rs, "id");
            List<CulturalFact> facts = findFactsForContext(contextId);
            return Optional.of(new CulturalContext(
                    contextId,
                    rs.getString("codigo"),
                    rs.getString("ciudad"),
                    rs.getString("lugar"),
                    rs.getString("titulo"),
                    rs.getString("descripcion"),
                    rs.getString("mensaje_animo"),
                    rs.getString("imagen_url"),
                    rs.getString("imagen_alt"),
                    rs.getString("credito_autor"),
                    rs.getString("credito_fuente"),
                    rs.getString("credito_licencia"),
                    rs.getString("credito_url"),
                    facts
            ));
        });
    }

    public List<CulturalFact> findFactsForContext(UUID contextId) {
        String sql = """
                SELECT id, parada_secuencia, orden, titulo, contenido, icono, fuente
                FROM curriculo_contexto_cultural_dato
                WHERE contexto_cultural_id = :contextId
                  AND estado = 'PUBLICADO'
                ORDER BY orden, parada_secuencia, id
                """;
        var parameters = new MapSqlParameterSource("contextId", contextId.toString());
        return jdbc.query(sql, parameters, (rs, rowNum) -> new CulturalFact(
                uuid(rs, "id"),
                (Integer) rs.getObject("parada_secuencia"),
                rs.getInt("orden"),
                rs.getString("titulo"),
                rs.getString("contenido"),
                rs.getString("icono"),
                rs.getString("fuente")
        ));
    }

    public record RouteHeader(
            UUID versionRouteId,
            UUID routeId,
            String title,
            Grade grade,
            Area area,
            Enrollment enrollment
    ) {
        public RouteDetailResponse toResponse(Progress progress, List<RouteNode> nodes) {
            return toResponse(progress, nodes, null, null);
        }

        public RouteDetailResponse toResponse(Progress progress, List<RouteNode> nodes, CulturalContext culturalContext, Curriculum curriculum) {
            return new RouteDetailResponse(versionRouteId, title, grade, area, enrollment, progress, nodes, culturalContext, curriculum);
        }
    }
}
