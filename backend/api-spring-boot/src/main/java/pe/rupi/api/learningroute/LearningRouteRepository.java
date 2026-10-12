package pe.rupi.api.learningroute;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
import pe.rupi.api.learningroute.LearningRouteDtos.RouteSearchResult;
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
    }

    public Optional<RouteCurriculum.Data> findCurriculum(UUID versionRouteId) {
        return findCurriculumData(versionRouteId);
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

    public List<RouteSearchResult> searchAlternativeRoutes(UUID studentId, String query, Integer gradeNumber, int limit) {
        List<Integer> grades = (gradeNumber != null && gradeNumber > 0) ? List.of(gradeNumber) : List.of();
        return searchAlternativeRoutes(studentId, query, grades, List.of(), limit);
    }

    public List<RouteSearchResult> searchAlternativeRoutes(
            UUID studentId,
            String query,
            List<Integer> gradeNumbers,
            List<String> areaCodes,
            int limit
    ) {
        String cleanQuery = (query == null) ? "" : query.trim();
        int maxResults = (limit <= 0 || limit > 50) ? 20 : limit;

        String[] rawTokens = cleanQuery.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");
        List<String> validTokens = java.util.Arrays.stream(rawTokens)
                .filter(t -> t != null && !t.isBlank())
                .toList();

        String ftsTerm = validTokens.isEmpty() ? "" : validTokens.stream()
                .map(t -> "+" + t + "*")
                .collect(java.util.stream.Collectors.joining(" "));
        String likeTerm = "%" + cleanQuery.toLowerCase() + "%";
        String prefixTerm = cleanQuery.toLowerCase() + "%";

        StringBuilder sql = new StringBuilder("""
                SELECT vr.id AS version_route_id,
                       r.titulo AS route_title,
                       r.descripcion AS route_description,
                       g.numero_grado,
                       g.nombre AS grade_name,
                       a.codigo AS area_code,
                       a.nombre AS area_name,
                       COUNT(DISTINCT n.id) AS total_nodes,
                       MAX(CASE WHEN i.id IS NOT NULL THEN 1 ELSE 0 END) AS is_enrolled,
                       MAX(COALESCE(i.estado, 'NO_INSCRITO')) AS enrollment_state
                FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                LEFT JOIN aprendizaje_inscripcion_ruta i
                       ON i.version_ruta_id = vr.id
                      AND i.estudiante_id = :studentId
                      AND i.estado IN ('ACTIVO', 'COMPLETADO')
                LEFT JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id
                WHERE vr.estado = 'PUBLICADO'
                  AND r.archivado_en IS NULL
                """);

        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("likeTerm", likeTerm)
                .addValue("prefixTerm", prefixTerm)
                .addValue("limit", maxResults)
                .addValue("cleanQuery", cleanQuery);

        if (!ftsTerm.isEmpty()) {
            params.addValue("ftsTerm", ftsTerm);
        }

        if (gradeNumbers != null && !gradeNumbers.isEmpty()) {
            sql.append(" AND g.numero_grado IN (:gradeNumbers) ");
            params.addValue("gradeNumbers", gradeNumbers);
        }

        if (areaCodes != null && !areaCodes.isEmpty()) {
            List<String> normalized = areaCodes.stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            if (!normalized.isEmpty()) {
                sql.append("""
                      AND (
                        LOWER(a.codigo) IN (:areaCodes)
                        OR LOWER(a.nombre) IN (:areaCodes)
                        OR (LOWER(a.codigo) = 'mat' AND ('matematica' IN (:areaCodes) OR 'matemática' IN (:areaCodes)))
                        OR (LOWER(a.codigo) = 'com' AND ('comunicacion' IN (:areaCodes) OR 'comunicación' IN (:areaCodes)))
                        OR (LOWER(a.codigo) = 'cyt' AND ('cyt' IN (:areaCodes) OR 'ciencia y tecnología' IN (:areaCodes) OR 'ciencia y tecnologia' IN (:areaCodes)))
                        OR (LOWER(a.codigo) = 'soc' AND ('personal social' IN (:areaCodes) OR 'soc' IN (:areaCodes)))
                        OR (LOWER(a.codigo) = 'art' AND ('arte y cultura' IN (:areaCodes) OR 'art' IN (:areaCodes)))
                      )
                    """);
                params.addValue("areaCodes", normalized);
            }
        }

        if (!cleanQuery.isEmpty()) {
            if (!ftsTerm.isEmpty()) {
                sql.append("""
                      AND (
                        LOWER(a.nombre) LIKE :likeTerm
                        OR LOWER(a.codigo) LIKE :likeTerm
                        OR LOWER(r.titulo) LIKE :likeTerm
                        OR LOWER(COALESCE(r.descripcion, '')) LIKE :likeTerm
                        OR MATCH(a.nombre) AGAINST(:ftsTerm IN BOOLEAN MODE)
                        OR MATCH(r.titulo, r.descripcion) AGAINST(:ftsTerm IN BOOLEAN MODE)
                      )
                    """);
            } else {
                sql.append("""
                      AND (
                        LOWER(a.nombre) LIKE :likeTerm
                        OR LOWER(a.codigo) LIKE :likeTerm
                        OR LOWER(r.titulo) LIKE :likeTerm
                        OR LOWER(COALESCE(r.descripcion, '')) LIKE :likeTerm
                      )
                    """);
            }
        }

        sql.append("""
                GROUP BY vr.id, r.titulo, r.descripcion, g.numero_grado, g.nombre, a.codigo, a.nombre
                ORDER BY
                  CASE
                    WHEN LOWER(a.nombre) = LOWER(:cleanQuery) THEN 1
                    WHEN LOWER(a.nombre) LIKE :prefixTerm THEN 2
                    WHEN LOWER(r.titulo) LIKE :prefixTerm THEN 3
                    ELSE 4
                  END,
                  g.numero_grado ASC,
                  a.nombre ASC,
                  r.titulo ASC
                LIMIT :limit
                """);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new RouteSearchResult(
                uuid(rs, "version_route_id"),
                rs.getString("route_title"),
                rs.getString("route_description"),
                rs.getInt("numero_grado"),
                rs.getString("grade_name"),
                rs.getString("area_code"),
                rs.getString("area_name"),
                rs.getLong("total_nodes"),
                rs.getInt("is_enrolled") == 1,
                rs.getString("enrollment_state")
        ));
    }

    public List<String> findSubjectSuggestions(String query, int limit) {
        String cleanQuery = (query == null) ? "" : query.trim();
        int maxResults = (limit <= 0 || limit > 20) ? 10 : limit;

        String[] rawTokens = cleanQuery.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");
        List<String> validTokens = java.util.Arrays.stream(rawTokens)
                .filter(t -> t != null && !t.isBlank())
                .toList();

        String ftsTerm = validTokens.isEmpty() ? "" : validTokens.stream()
                .map(t -> "+" + t + "*")
                .collect(java.util.stream.Collectors.joining(" "));
        String likeTerm = "%" + cleanQuery.toLowerCase() + "%";
        String prefixTerm = cleanQuery.toLowerCase() + "%";

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT a.nombre
                FROM curriculo_area a
                """);

        var params = new MapSqlParameterSource()
                .addValue("cleanQuery", cleanQuery)
                .addValue("likeTerm", likeTerm)
                .addValue("prefixTerm", prefixTerm)
                .addValue("limit", maxResults);

        if (cleanQuery.isEmpty()) {
            // Sin filtro, devolver catálogo
        } else if (!ftsTerm.isEmpty()) {
            sql.append("""
                    WHERE LOWER(a.nombre) LIKE :likeTerm
                       OR LOWER(a.codigo) LIKE :likeTerm
                       OR MATCH(a.nombre) AGAINST(:ftsTerm IN BOOLEAN MODE)
                    """);
            params.addValue("ftsTerm", ftsTerm);
        } else {
            sql.append("""
                    WHERE LOWER(a.nombre) LIKE :likeTerm
                       OR LOWER(a.codigo) LIKE :likeTerm
                    """);
        }

        sql.append("""
                ORDER BY
                  CASE
                    WHEN LOWER(a.nombre) LIKE :prefixTerm THEN 1
                    ELSE 2
                  END,
                  a.nombre ASC
                LIMIT :limit
                """);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> rs.getString("nombre"));
    }

    public boolean isPublishedRoute(UUID versionRouteId) {
        String sql = """
                SELECT COUNT(*) FROM aprendizaje_version_ruta vr
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                WHERE vr.id = :versionRouteId AND vr.estado = 'PUBLICADO' AND r.archivado_en IS NULL
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("versionRouteId", versionRouteId.toString()), Integer.class);
        return count != null && count > 0;
    }

    public Optional<AccessCodeDetails> findAccessCode(String normalizedCode) {
        String sql = """
                SELECT c.id AS code_id,
                       c.version_ruta_id,
                       c.valido_desde,
                       c.valido_hasta,
                       c.maximo_usos,
                       c.revocado_en,
                       r.titulo AS route_title,
                       r.archivado_en,
                       vr.estado AS route_state,
                       g.numero_grado,
                       g.nombre AS grade_name,
                       a.codigo AS area_code,
                       a.nombre AS area_name,
                       (SELECT COUNT(*) FROM aprendizaje_inscripcion_ruta i WHERE i.codigo_acceso_id = c.id) AS current_uses
                FROM aprendizaje_codigo_acceso_ruta c
                JOIN aprendizaje_version_ruta vr ON vr.id = c.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                WHERE c.hash_codigo = UNHEX(SHA2(:code, 256))
                """;
        var params = new MapSqlParameterSource("code", normalizedCode);
        return jdbc.query(sql, params, rs -> {
            if (!rs.next()) return Optional.empty();
            Timestamp fromTs = rs.getTimestamp("valido_desde");
            Timestamp untilTs = rs.getTimestamp("valido_hasta");
            Timestamp revokedTs = rs.getTimestamp("revocado_en");
            Timestamp archivedTs = rs.getTimestamp("archivado_en");
            return Optional.of(new AccessCodeDetails(
                    uuid(rs, "code_id"),
                    uuid(rs, "version_ruta_id"),
                    rs.getString("route_title"),
                    new Grade(rs.getInt("numero_grado"), rs.getString("grade_name")),
                    new Area(rs.getString("area_code"), rs.getString("area_name")),
                    fromTs != null ? fromTs.toInstant() : null,
                    untilTs != null ? untilTs.toInstant() : null,
                    rs.getObject("maximo_usos") != null ? rs.getInt("maximo_usos") : null,
                    revokedTs != null ? revokedTs.toInstant() : null,
                    "PUBLICADO".equalsIgnoreCase(rs.getString("route_state")),
                    archivedTs != null,
                    rs.getLong("current_uses")
            ));
        });
    }

    public boolean isStudentEnrolled(UUID studentId, UUID versionRouteId) {
        String sql = """
                SELECT COUNT(*) FROM aprendizaje_inscripcion_ruta
                WHERE estudiante_id = :studentId AND version_ruta_id = :versionRouteId
                  AND estado IN ('ACTIVO', 'COMPLETADO')
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("versionRouteId", versionRouteId.toString()), Integer.class);
        return count != null && count > 0;
    }

    public void linkAccessCodeToEnrollment(UUID studentId, UUID versionRouteId, UUID accessCodeId) {
        String sql = """
                UPDATE aprendizaje_inscripcion_ruta
                SET codigo_acceso_id = COALESCE(codigo_acceso_id, :accessCodeId)
                WHERE estudiante_id = :studentId AND version_ruta_id = :versionRouteId
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("versionRouteId", versionRouteId.toString())
                .addValue("accessCodeId", accessCodeId != null ? accessCodeId.toString() : null));
    }

    public void enrollStudentInRoute(UUID studentId, UUID versionRouteId) {
        enrollStudentWithAccessCode(studentId, versionRouteId, null);
    }

    public void enrollStudentWithAccessCode(UUID studentId, UUID versionRouteId, UUID accessCodeId) {
        UUID enrollmentId = UUID.randomUUID();
        String sqlEnroll = """
                INSERT IGNORE INTO aprendizaje_inscripcion_ruta
                (id, version_ruta_id, estudiante_id, codigo_acceso_id, estado, inscrito_en, ultima_visita_en)
                VALUES (:id, :versionRouteId, :studentId, :accessCodeId, 'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """;
        jdbc.update(sqlEnroll, new MapSqlParameterSource()
                .addValue("id", enrollmentId.toString())
                .addValue("versionRouteId", versionRouteId.toString())
                .addValue("studentId", studentId.toString())
                .addValue("accessCodeId", accessCodeId != null ? accessCodeId.toString() : null));

        String sqlGetEnrollment = """
                SELECT id FROM aprendizaje_inscripcion_ruta
                WHERE version_ruta_id = :versionRouteId AND estudiante_id = :studentId
                """;
        List<String> enrollments = jdbc.query(sqlGetEnrollment, new MapSqlParameterSource()
                .addValue("versionRouteId", versionRouteId.toString())
                .addValue("studentId", studentId.toString()), (rs, rowNum) -> rs.getString("id"));
        if (enrollments.isEmpty()) {
            return;
        }
        String realEnrollmentId = enrollments.getFirst();

        if (accessCodeId != null) {
            jdbc.update("""
                    UPDATE aprendizaje_inscripcion_ruta
                    SET codigo_acceso_id = COALESCE(codigo_acceso_id, :accessCodeId)
                    WHERE id = :enrollmentId
                    """, new MapSqlParameterSource()
                    .addValue("accessCodeId", accessCodeId.toString())
                    .addValue("enrollmentId", realEnrollmentId));
        }

        record NodeSeq(String id, int sequence) {}
        String sqlNodes = """
                SELECT id, numero_secuencia FROM aprendizaje_nodo_ruta
                WHERE version_ruta_id = :versionRouteId
                ORDER BY numero_secuencia ASC
                """;
        List<NodeSeq> nodes = jdbc.query(sqlNodes, new MapSqlParameterSource("versionRouteId", versionRouteId.toString()),
                (rs, rowNum) -> new NodeSeq(rs.getString("id"), rs.getInt("numero_secuencia")));

        for (int i = 0; i < nodes.size(); i++) {
            var n = nodes.get(i);
            String state = (i == 0) ? "DISPONIBLE" : "BLOQUEADO";
            String sqlProgress = """
                    INSERT IGNORE INTO aprendizaje_progreso_nodo
                    (inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en)
                    VALUES (:enrollmentId, :versionRouteId, :nodeId, :state,
                            CASE WHEN :state = 'DISPONIBLE' THEN UTC_TIMESTAMP(6) ELSE NULL END)
                    """;
            jdbc.update(sqlProgress, new MapSqlParameterSource()
                    .addValue("enrollmentId", realEnrollmentId)
                    .addValue("versionRouteId", versionRouteId.toString())
                    .addValue("nodeId", n.id())
                    .addValue("state", state));
        }

        if (!nodes.isEmpty()) {
            jdbc.update("""
                    UPDATE aprendizaje_inscripcion_ruta
                    SET ultimo_nodo_visitado_id = COALESCE(ultimo_nodo_visitado_id, :firstNode)
                    WHERE id = :enrollmentId
                    """, new MapSqlParameterSource("firstNode", nodes.getFirst().id())
                    .addValue("enrollmentId", realEnrollmentId));
        }
    }

    public record AccessCodeDetails(
            UUID id,
            UUID versionRouteId,
            String routeTitle,
            Grade grade,
            Area area,
            Instant validFrom,
            Instant validUntil,
            Integer maxUses,
            Instant revokedAt,
            boolean routePublished,
            boolean routeArchived,
            long currentUses
    ) {}


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
