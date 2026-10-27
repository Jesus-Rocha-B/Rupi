package pe.rupi.api.learningroute;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationService;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;
import pe.rupi.api.learningroute.LearningRouteDtos.ActivityCompleteResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.BadgeDto;
import pe.rupi.api.learningroute.LearningRouteDtos.ContentBlock;
import pe.rupi.api.learningroute.LearningRouteDtos.NextUnlockedNode;

@Service
public class ActivityService {
    private final NamedParameterJdbcTemplate jdbc;
    private final EvaluationService evaluationService;
    private final pe.rupi.api.gamification.DailyGoalProgressService goalProgressService;

    public ActivityService(NamedParameterJdbcTemplate jdbc) {
        this(jdbc, new EvaluationService(jdbc), null);
    }

    public ActivityService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService) {
        this(jdbc, evaluationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActivityService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService, pe.rupi.api.gamification.DailyGoalProgressService goalProgressService) {
        this.jdbc = jdbc;
        this.evaluationService = evaluationService;
        this.goalProgressService = goalProgressService;
    }


    public record Block(String id, String type, String text, String url, String accessibleText) {}
    public record Activity(
            String nodeId,
            String activityId,
            String title,
            String instructions,
            String state,
            List<Block> blocks,
            List<QuestionDto> questions
    ) {}
    record Access(String enrollmentId, String activityId, String title, String instructions, String state, java.sql.Timestamp availableFrom, java.sql.Timestamp lockedFrom) {
        Access(String enrollmentId, String activityId, String title, String instructions, String state) {
            this(enrollmentId, activityId, title, instructions, state, null, null);
        }
    }

    /**
     * Crea las filas de progreso que falten en la inscripción (paradas agregadas después de inscribirse o
     * inscripciones antiguas sin filas). Sin fila, abrir la parada 1 funcionaba pero completarla respondía 403,
     * y desbloquear la siguiente era un UPDATE sin efecto. Una parada nace DISPONIBLE si es la primera o si la
     * anterior ya está completada. INSERT IGNORE: nunca toca filas existentes.
     */
    private void ensureProgressRows(MapSqlParameterSource params) {
        jdbc.update("""
                INSERT IGNORE INTO aprendizaje_progreso_nodo
                    (inscripcion_id, version_ruta_id, nodo_ruta_id, estado, desbloqueado_en)
                SELECT i.id, i.version_ruta_id, n.id,
                       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN 'DISPONIBLE' ELSE 'BLOQUEADO' END,
                       CASE WHEN prev.id IS NULL OR pp.estado = 'COMPLETADO' THEN UTC_TIMESTAMP(6) ELSE NULL END
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = i.version_ruta_id
                LEFT JOIN aprendizaje_nodo_ruta prev
                       ON prev.version_ruta_id = i.version_ruta_id
                      AND prev.numero_secuencia = (
                          SELECT MAX(x.numero_secuencia) FROM aprendizaje_nodo_ruta x
                          WHERE x.version_ruta_id = i.version_ruta_id AND x.numero_secuencia < n.numero_secuencia)
                LEFT JOIN aprendizaje_progreso_nodo pp
                       ON pp.inscripcion_id = i.id AND pp.nodo_ruta_id = prev.id
                WHERE i.estudiante_id = :student
                  AND i.version_ruta_id = :route
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                """, params);
    }

    @Transactional
    public Activity start(UUID studentId, UUID routeId, UUID nodeId) {
        var params = new MapSqlParameterSource("student", studentId.toString())
                .addValue("route", routeId.toString())
                .addValue("node", nodeId.toString());

        ensureProgressRows(params);

        // Lock the enrollment and progress together. Repeated starts preserve first access and completion.
        var access = jdbc.query("""
                SELECT i.id AS enrollment_id, va.id AS activity_id, va.titulo, va.instrucciones,
                       COALESCE(p.estado, CASE WHEN n.numero_secuencia = 1 THEN 'DISPONIBLE' ELSE 'BLOQUEADO' END) AS estado,
                       n.disponible_desde, n.bloqueado_desde
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_nodo_ruta n ON n.id = :node AND n.version_ruta_id = vr.id
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.version_ruta_id = vr.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :student
                  AND i.version_ruta_id = :route
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                  AND va.estado = 'PUBLICADO'
                FOR UPDATE
                """, params, (rs, rowNum) -> new Access(
                rs.getString("enrollment_id"),
                rs.getString("activity_id"),
                rs.getString("titulo"),
                rs.getString("instrucciones"),
                rs.getString("estado"),
                rs.getTimestamp("disponible_desde"),
                rs.getTimestamp("bloqueado_desde")
        )).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!List.of("DISPONIBLE", "EN_CURSO", "COMPLETADO").contains(access.state())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El nivel solicitado aún está bloqueado.");
        }

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        if (access.availableFrom() != null && now.before(access.availableFrom())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta parada aún no está disponible según la programación del docente.");
        }
        if (access.lockedFrom() != null && now.after(access.lockedFrom())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El plazo para acceder a esta parada ha concluido.");
        }

        var blocks = jdbc.query("""
                SELECT b.id, b.tipo,
                       COALESCE(b.texto_cuerpo, '') AS texto,
                       COALESCE(m.url_externa, m.clave_almacenamiento, '') AS url_recurso,
                       b.texto_accesibilidad AS texto_accesible
                FROM aprendizaje_bloque_contenido b
                LEFT JOIN recurso_archivo_multimedia m ON m.id = b.archivo_multimedia_id
                WHERE b.version_actividad_id = :activity
                ORDER BY b.numero_secuencia
                """, new MapSqlParameterSource("activity", access.activityId()), (rs, rowNum) -> new Block(
                rs.getString("id"),
                rs.getString("tipo"),
                rs.getString("texto"),
                rs.getString("url_recurso"),
                rs.getString("texto_accesible")
        ));

        if (blocks.isEmpty() && (access.instructions() == null || access.instructions().isBlank())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La actividad no tiene contenido disponible.");
        }

        if ("DISPONIBLE".equalsIgnoreCase(access.state())) {
            jdbc.update("""
                    UPDATE aprendizaje_progreso_nodo
                    SET estado = 'EN_CURSO',
                        primer_acceso_en = COALESCE(primer_acceso_en, UTC_TIMESTAMP(6))
                    WHERE inscripcion_id = :enrollment AND nodo_ruta_id = :node
                    """, new MapSqlParameterSource("enrollment", access.enrollmentId())
                    .addValue("node", nodeId.toString()));
        }

        jdbc.update("""
                UPDATE aprendizaje_inscripcion_ruta
                SET ultimo_nodo_visitado_id = :node, ultima_visita_en = UTC_TIMESTAMP(6)
                WHERE id = :enrollment
                """, new MapSqlParameterSource("enrollment", access.enrollmentId())
                .addValue("node", nodeId.toString()));

        List<QuestionDto> questions = List.of();
        try {
            questions = evaluationService.findQuestionsForActivity(UUID.fromString(access.activityId()));
        } catch (IllegalArgumentException ignored) {
            // Manejar si el activityId es un identificador no UUID durante tests
        }
        String currentState = "DISPONIBLE".equalsIgnoreCase(access.state()) ? "EN_CURSO" : access.state();
        return new Activity(nodeId.toString(), access.activityId(), access.title(), access.instructions(), currentState, blocks, questions);
    }

    @Transactional
    public ActivityCompleteResponse complete(UUID studentId, UUID routeId, UUID nodeId) {
        var params = new MapSqlParameterSource("student", studentId.toString())
                .addValue("route", routeId.toString())
                .addValue("node", nodeId.toString());

        record NodeAccess(String enrollmentId, String activityVersionId, int sequence, String currentState) {}

        ensureProgressRows(params);

        var nodeAccess = jdbc.query("""
                SELECT i.id AS enrollment_id, n.version_actividad_id, n.numero_secuencia,
                       COALESCE(p.estado, 'BLOQUEADO') AS estado
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_nodo_ruta n ON n.id = :node AND n.version_ruta_id = vr.id
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id AND va.estado = 'PUBLICADO'
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.version_ruta_id = vr.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :student
                  AND i.version_ruta_id = :route
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                FOR UPDATE
                """, params, (rs, rowNum) -> new NodeAccess(
                rs.getString("enrollment_id"),
                rs.getString("version_actividad_id"),
                rs.getInt("numero_secuencia"),
                rs.getString("estado")
        )).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if ("BLOQUEADO".equalsIgnoreCase(nodeAccess.currentState())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes completar un nivel bloqueado.");
        }

        boolean wasAlreadyCompleted = "COMPLETADO".equalsIgnoreCase(nodeAccess.currentState());
        int experienceToAward = wasAlreadyCompleted ? 0 : 50;

        if (!wasAlreadyCompleted) {
            // 1. Marcar nodo como completado
            jdbc.update("""
                    UPDATE aprendizaje_progreso_nodo
                    SET estado = 'COMPLETADO',
                        completado_en = UTC_TIMESTAMP(6)
                    WHERE inscripcion_id = :enrollment AND nodo_ruta_id = :node
                    """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId())
                    .addValue("node", nodeId.toString()));

            // 2. Registrar puntos de experiencia (XP) de forma idempotente en el libro de movimientos
            String idempotencyKey = UUID.nameUUIDFromBytes(
                    (studentId + ":" + nodeId + ":COMPLETADO").getBytes(StandardCharsets.UTF_8)
            ).toString();

            jdbc.update("""
                    INSERT IGNORE INTO gamificacion_movimiento_experiencia
                    (id, usuario_id, cantidad, motivo, referencia_tipo, referencia_id, evento_idempotencia, ocurrido_en)
                    VALUES (:id, :userId, :amount, 'ACTIVIDAD', 'ACTIVIDAD', :refId, :idempotency, UTC_TIMESTAMP(6))
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("userId", studentId.toString())
                    .addValue("amount", experienceToAward)
                    .addValue("refId", nodeAccess.activityVersionId())
                    .addValue("idempotency", idempotencyKey));

            if (goalProgressService != null) {
                goalProgressService.recordCompletedActivity(studentId.toString());
            }
        }

        // 3. Buscar y desbloquear el siguiente nodo
        record NextNode(UUID id, int sequence, String title, String state) {}

        var nextNodeOpt = jdbc.query("""
                SELECT n.id, n.numero_secuencia, va.titulo, COALESCE(p.estado, 'BLOQUEADO') AS estado
                FROM aprendizaje_nodo_ruta n
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = :enrollment
                      AND p.version_ruta_id = n.version_ruta_id
                      AND p.nodo_ruta_id = n.id
                WHERE n.version_ruta_id = :route
                  AND n.numero_secuencia > :currentSeq
                ORDER BY n.numero_secuencia ASC
                LIMIT 1
                """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId())
                .addValue("route", routeId.toString())
                .addValue("currentSeq", nodeAccess.sequence()), (rs, rowNum) -> new NextNode(
                UUID.fromString(rs.getString("id")),
                rs.getInt("numero_secuencia"),
                rs.getString("titulo"),
                rs.getString("estado")
        )).stream().findFirst();

        NextUnlockedNode nextUnlocked = null;
        boolean isRouteCompleted = nextNodeOpt.isEmpty();
        int routeBonusExperience = 0;

        if (nextNodeOpt.isPresent()) {
            var next = nextNodeOpt.get();
            if ("BLOQUEADO".equalsIgnoreCase(next.state())) {
                jdbc.update("""
                        UPDATE aprendizaje_progreso_nodo
                        SET estado = 'DISPONIBLE',
                            desbloqueado_en = UTC_TIMESTAMP(6)
                        WHERE inscripcion_id = :enrollment AND nodo_ruta_id = :nodeId
                        """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId())
                        .addValue("nodeId", next.id().toString()));
            }
            jdbc.update("""
                    UPDATE aprendizaje_inscripcion_ruta
                    SET ultimo_nodo_visitado_id = :nextNodeId,
                        ultima_visita_en = UTC_TIMESTAMP(6)
                    WHERE id = :enrollment
                    """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId())
                    .addValue("nextNodeId", next.id().toString()));
            nextUnlocked = new NextUnlockedNode(next.id(), next.sequence(), next.title());
        } else {
            // Si no hay más nodos, la ruta completa queda como COMPLETADA (HU-08)
            jdbc.update("""
                    UPDATE aprendizaje_inscripcion_ruta
                    SET estado = 'COMPLETADO',
                        completado_en = UTC_TIMESTAMP(6)
                    WHERE id = :enrollment
                    """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId()));

            if (!wasAlreadyCompleted) {
                // Bonificación especial de fin de ruta (+100 XP) en el libro de movimientos
                routeBonusExperience = 100;
                String routeBonusIdempotency = UUID.nameUUIDFromBytes(
                        (studentId + ":" + routeId + ":GRADUACION_RUTA").getBytes(StandardCharsets.UTF_8)
                ).toString();

                jdbc.update("""
                        INSERT IGNORE INTO gamificacion_movimiento_experiencia
                        (id, usuario_id, cantidad, motivo, referencia_tipo, referencia_id, evento_idempotencia, ocurrido_en)
                        VALUES (:id, :userId, :amount, 'INSIGNIA', 'RUTA', :refId, :idempotency, UTC_TIMESTAMP(6))
                        """, new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID().toString())
                        .addValue("userId", studentId.toString())
                        .addValue("amount", routeBonusExperience)
                        .addValue("refId", routeId.toString())
                        .addValue("idempotency", routeBonusIdempotency));
            }
        }

        // 4. Calcular XP total acumulado del estudiante (incluyendo bono si aplica)
        Integer totalXp = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :userId
                """, new MapSqlParameterSource("userId", studentId.toString()), Integer.class);

        // 5. Gestión de Insignias Escolares (HU-07 y HU-08)
        BadgeDto badgeEarned = null;
        if (!wasAlreadyCompleted) {
            if (isRouteCompleted) {
                // Gran Insignia de Culminación: Gran Gallito de Oro (HU-08)
                String grandBadgeId = "ba000000-0000-4000-8000-000000000002";
                jdbc.update("""
                        INSERT IGNORE INTO gamificacion_insignia (id, codigo, nombre, descripcion, criterio, activa)
                        VALUES (:id, 'GRAN_TUNQUI_ORO', 'Gran Gallito de Oro',
                                '¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.', '{}', TRUE)
                        """, new MapSqlParameterSource("id", grandBadgeId));

                jdbc.update("""
                        INSERT IGNORE INTO gamificacion_insignia_usuario (usuario_id, insignia_id, obtenida_en)
                        VALUES (:userId, :badgeId, UTC_TIMESTAMP(6))
                        """, new MapSqlParameterSource("userId", studentId.toString()).addValue("badgeId", grandBadgeId));

                badgeEarned = new BadgeDto(
                        "GRAN_TUNQUI_ORO",
                        "Gran Gallito de Oro",
                        "¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.",
                        "feather"
                );
            } else {
                // Primera insignia escolar de lección: Explorador de Ayacucho (HU-07)
                String badgeId = "ba000000-0000-4000-8000-000000000001";
                jdbc.update("""
                        INSERT IGNORE INTO gamificacion_insignia (id, codigo, nombre, descripcion, criterio, activa)
                        VALUES (:id, 'EXPLORADOR_AYACUCHO', 'Explorador de Ayacucho',
                                'Completaste tu primera parada escolar y superaste los desafíos de la lección.', '{}', TRUE)
                        """, new MapSqlParameterSource("id", badgeId));

                int inserted = jdbc.update("""
                        INSERT IGNORE INTO gamificacion_insignia_usuario (usuario_id, insignia_id, obtenida_en)
                        VALUES (:userId, :badgeId, UTC_TIMESTAMP(6))
                        """, new MapSqlParameterSource("userId", studentId.toString()).addValue("badgeId", badgeId));

                if (inserted > 0) {
                    badgeEarned = new BadgeDto(
                            "EXPLORADOR_AYACUCHO",
                            "Explorador de Ayacucho",
                            "Completaste tu primera parada escolar y superaste los desafíos de la lección.",
                            "medal"
                    );
                }
            }
        }

        String message;
        if (isRouteCompleted) {
            Integer totalRouteNodes = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = :route
                    """, new MapSqlParameterSource("route", routeId.toString()), Integer.class);
            int totalCount = (totalRouteNodes != null && totalRouteNodes > 0) ? totalRouteNodes : nodeAccess.sequence();
            message = wasAlreadyCompleted
                    ? "¡Misión cumplida! Ya habías culminado todas las paradas de la ruta escolar. ¡Sigue repasando cuando quieras!"
                    : "¡Misión cumplida! Completaste las " + totalCount + " paradas de la ruta escolar como un auténtico Gallito de las Rocas. ¡Ganaste la Gran Insignia 'Gran Gallito de Oro' (+100 XP) en tu vitrina de perfil escolar!";
        } else {
            message = wasAlreadyCompleted
                    ? "¡Excelente repaso! Esta parada ya estaba completada en tu aventura."
                    : (nextUnlocked != null
                    ? "¡Felicitaciones! Has completado esta parada, sumaste " + experienceToAward + " XP y desbloqueaste la siguiente aventura con Rupi."
                    : "¡Misión cumplida! Has completado todas las paradas de esta ruta de aprendizaje.");
        }

        return new ActivityCompleteResponse(
                nodeId,
                "COMPLETADO",
                experienceToAward,
                totalXp != null ? totalXp : 0,
                nextUnlocked,
                message,
                badgeEarned,
                isRouteCompleted,
                routeBonusExperience
        );
    }

    public LearningRouteDtos.RouteDiplomaResponse getRouteDiploma(UUID studentId, UUID versionRouteId) {
        String sqlRoute = """
                SELECT vr.id, r.titulo AS route_title, g.numero_grado, g.nombre AS grade_name, a.nombre AS area_name,
                       i.estado AS enrollment_state, i.completado_en
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                WHERE i.estudiante_id = :studentId AND vr.id = :versionRouteId
                """;
        record RouteData(UUID versionRouteId, String routeTitle, int gradeNumber, String gradeName,
                         String areaName, String enrollmentState, String completedAt) {}

        var routeDataList = jdbc.query(sqlRoute, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("versionRouteId", versionRouteId.toString()),
                (rs, rowNum) -> new RouteData(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("route_title"),
                        rs.getInt("numero_grado"),
                        rs.getString("grade_name"),
                        rs.getString("area_name"),
                        rs.getString("enrollment_state"),
                        rs.getString("completado_en")
                ));

        if (routeDataList.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción en la ruta no encontrada.");
        }
        var routeData = routeDataList.getFirst();

        String sqlCounts = """
                SELECT
                    (SELECT COUNT(*) FROM aprendizaje_nodo_ruta WHERE version_ruta_id = :versionRouteId) AS total_nodes,
                    (SELECT COUNT(*) FROM aprendizaje_progreso_nodo p
                     JOIN aprendizaje_inscripcion_ruta i ON i.id = p.inscripcion_id
                     WHERE i.estudiante_id = :studentId AND i.version_ruta_id = :versionRouteId AND p.estado = 'COMPLETADO') AS completed_nodes
                """;
        record NodeCounts(int total, int completed) {}
        var counts = jdbc.queryForObject(sqlCounts, new MapSqlParameterSource()
                .addValue("studentId", studentId.toString())
                .addValue("versionRouteId", versionRouteId.toString()),
                (rs, rowNum) -> new NodeCounts(rs.getInt("total_nodes"), rs.getInt("completed_nodes")));

        int totalNodes = counts != null ? counts.total() : 10;
        int completedNodes = counts != null ? counts.completed() : 0;

        if (completedNodes < totalNodes && !"COMPLETADO".equalsIgnoreCase(routeData.enrollmentState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aún no has completado todas las paradas de la ruta para obtener tu diploma escolar.");
        }

        String sqlStudentName = """
                SELECT COALESCE(NULLIF(TRIM(CONCAT(COALESCE(p.nombre, ''), ' ', COALESCE(p.apellido, ''))), ''), p.alias_publico, u.nombre_usuario) AS student_name
                FROM identidad_cuenta_usuario u
                LEFT JOIN identidad_perfil_usuario p ON p.usuario_id = u.id
                WHERE u.id = :studentId
                """;
        List<String> names = jdbc.query(sqlStudentName, new MapSqlParameterSource("studentId", studentId.toString()),
                (rs, rowNum) -> rs.getString("student_name"));
        String studentName = names.isEmpty() || names.getFirst() == null ? "Estudiante" : names.getFirst();

        String sqlSchool = """
                SELECT i.nombre
                FROM escuela_membresia_aula m
                JOIN escuela_aula a ON a.id = m.aula_id
                JOIN escuela_institucion i ON i.id = a.institucion_id
                WHERE m.usuario_id = :studentId AND m.estado = 'ACTIVO'
                LIMIT 1
                """;
        List<String> schools = jdbc.query(sqlSchool, new MapSqlParameterSource("studentId", studentId.toString()),
                (rs, rowNum) -> rs.getString("nombre"));
        String schoolName = schools.isEmpty() ? "Colegio Primario Bicentenario" : schools.getFirst();

        Integer totalXp = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                """, new MapSqlParameterSource("studentId", studentId.toString()), Integer.class);

        String sqlCompetencies = """
                SELECT DISTINCT c.codigo, c.nombre
                FROM curriculo_competencia c
                JOIN curriculo_unidad_competencia uc ON uc.competencia_id = c.id
                JOIN aprendizaje_nodo_ruta n ON n.unidad_id = uc.unidad_id
                WHERE n.version_ruta_id = :versionRouteId
                ORDER BY c.codigo ASC
                """;
        List<LearningRouteDtos.Competency> competencies = jdbc.query(sqlCompetencies,
                new MapSqlParameterSource("versionRouteId", versionRouteId.toString()),
                (rs, rowNum) -> new LearningRouteDtos.Competency(rs.getString("codigo"), rs.getString("nombre")));

        if (competencies.isEmpty()) {
            String areaLower = routeData.areaName() != null ? routeData.areaName().toLowerCase() : "";
            if (areaLower.contains("comunicaci") || areaLower.contains("com")) {
                competencies = List.of(
                        new LearningRouteDtos.Competency("COM-01", "Se comunica oralmente en su lengua materna"),
                        new LearningRouteDtos.Competency("COM-02", "Lee diversos tipos de textos escritos en su lengua materna"),
                        new LearningRouteDtos.Competency("COM-03", "Escribe diversos tipos de textos en su lengua materna")
                );
            } else if (areaLower.contains("ciencia") || areaLower.contains("cyt") || areaLower.contains("tecnolog")) {
                competencies = List.of(
                        new LearningRouteDtos.Competency("CYT-01", "Indaga mediante métodos científicos para construir sus conocimientos"),
                        new LearningRouteDtos.Competency("CYT-02", "Explica el mundo físico basándose en conocimientos sobre los seres vivos, materia y energía, biodiversidad, Tierra y universo"),
                        new LearningRouteDtos.Competency("CYT-03", "Diseña y construye soluciones tecnológicas para resolver problemas de su entorno")
                );
            } else if (areaLower.contains("personal") || areaLower.contains("social") || areaLower.contains("soc")) {
                competencies = List.of(
                        new LearningRouteDtos.Competency("SOC-01", "Construye su identidad"),
                        new LearningRouteDtos.Competency("SOC-02", "Convive y participa democráticamente en la búsqueda del bien común")
                );
            } else {
                competencies = List.of(
                        new LearningRouteDtos.Competency("MAT-01", "Resuelve problemas de cantidad"),
                        new LearningRouteDtos.Competency("MAT-02", "Resuelve problemas de regularidad, equivalencia y cambio"),
                        new LearningRouteDtos.Competency("MAT-03", "Resuelve problemas de forma, movimiento y localización"),
                        new LearningRouteDtos.Competency("MAT-04", "Resuelve problemas de gestión de datos e incertidumbre")
                );
            }
        }

        BadgeDto graduationBadge = new BadgeDto(
                "GRAN_TUNQUI_ORO",
                "Gran Gallito de Oro",
                "¡Alcanzaste la cima de la ruta! Volaste como el gallito de las rocas por toda la aventura escolar de Ayacucho.",
                "feather"
        );

        String completedAt = routeData.completedAt() != null ? routeData.completedAt() : java.time.Instant.now().toString();
        String honorMessage = "¡Honor al mérito! Con valentía, alegría y perseverancia, superaste todos los retos de la ruta escolar con la fuerza y agilidad del Gallito de las Rocas.";

        return new LearningRouteDtos.RouteDiplomaResponse(
                routeData.versionRouteId(),
                routeData.routeTitle(),
                studentName,
                schoolName,
                routeData.gradeNumber(),
                routeData.gradeName(),
                routeData.areaName(),
                completedNodes,
                totalNodes,
                totalXp != null ? totalXp : 0,
                completedAt,
                graduationBadge,
                competencies,
                honorMessage
        );
    }
}
