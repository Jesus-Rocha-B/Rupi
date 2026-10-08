package pe.rupi.api.learningroute;

import java.nio.charset.StandardCharsets;
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

    public ActivityService(NamedParameterJdbcTemplate jdbc) {
        this(jdbc, new EvaluationService(jdbc));
    }

    public ActivityService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService) {
        this.jdbc = jdbc;
        this.evaluationService = evaluationService;
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
    record Access(String enrollmentId, String activityId, String title, String instructions, String state) {}

    @Transactional
    public Activity start(UUID studentId, UUID routeId, UUID nodeId) {
        var params = new MapSqlParameterSource("student", studentId.toString())
                .addValue("route", routeId.toString())
                .addValue("node", nodeId.toString());

        // Lock the enrollment and progress together. Repeated starts preserve first access and completion.
        var access = jdbc.query("""
                SELECT i.id AS enrollment_id, va.id AS activity_id, va.titulo, va.instrucciones,
                       COALESCE(p.estado, 'BLOQUEADO') AS estado
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
                FOR UPDATE
                """, params, (rs, _) -> new Access(
                rs.getString("enrollment_id"),
                rs.getString("activity_id"),
                rs.getString("titulo"),
                rs.getString("instrucciones"),
                rs.getString("estado")
        )).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if ("BLOQUEADO".equalsIgnoreCase(access.state())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El nivel solicitado aún está bloqueado.");
        }

        if ("DISPONIBLE".equalsIgnoreCase(access.state())) {
            jdbc.update("""
                    UPDATE aprendizaje_progreso_nodo
                    SET estado = 'EN_CURSO',
                        primer_acceso_en = COALESCE(primer_acceso_en, CURRENT_TIMESTAMP(6))
                    WHERE inscripcion_id = :enrollment AND nodo_ruta_id = :node
                    """, new MapSqlParameterSource("enrollment", access.enrollmentId())
                    .addValue("node", nodeId.toString()));
        }

        jdbc.update("""
                UPDATE aprendizaje_inscripcion_ruta
                SET ultimo_nodo_visitado_id = :node
                WHERE id = :enrollment
                """, new MapSqlParameterSource("enrollment", access.enrollmentId())
                .addValue("node", nodeId.toString()));

        var blocks = jdbc.query("""
                SELECT id, tipo, texto, url_recurso, texto_accesible
                FROM aprendizaje_bloque_contenido
                WHERE version_actividad_id = :activity
                ORDER BY numero_secuencia
                """, new MapSqlParameterSource("activity", access.activityId()), (rs, _) -> new Block(
                rs.getString("id"),
                rs.getString("tipo"),
                rs.getString("texto"),
                rs.getString("url_recurso"),
                rs.getString("texto_accesible")
        ));

        var questions = evaluationService.findQuestionsForActivity(UUID.fromString(access.activityId()));
        String currentState = "DISPONIBLE".equalsIgnoreCase(access.state()) ? "EN_CURSO" : access.state();
        return new Activity(nodeId.toString(), access.activityId(), access.title(), access.instructions(), currentState, blocks, questions);
    }

    @Transactional
    public ActivityCompleteResponse complete(UUID studentId, UUID routeId, UUID nodeId) {
        var params = new MapSqlParameterSource("student", studentId.toString())
                .addValue("route", routeId.toString())
                .addValue("node", nodeId.toString());

        record NodeAccess(String enrollmentId, String activityVersionId, int sequence, String currentState) {}

        var nodeAccess = jdbc.query("""
                SELECT i.id AS enrollment_id, n.version_actividad_id, n.numero_secuencia,
                       COALESCE(p.estado, 'BLOQUEADO') AS estado
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_nodo_ruta n ON n.id = :node AND n.version_ruta_id = vr.id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.version_ruta_id = vr.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :student
                  AND i.version_ruta_id = :route
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                FOR UPDATE
                """, params, (rs, _) -> new NodeAccess(
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
                        completado_en = CURRENT_TIMESTAMP(6)
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
                    VALUES (:id, :userId, :amount, 'ACTIVIDAD', 'ACTIVIDAD', :refId, :idempotency, CURRENT_TIMESTAMP(6))
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("userId", studentId.toString())
                    .addValue("amount", experienceToAward)
                    .addValue("refId", nodeAccess.activityVersionId())
                    .addValue("idempotency", idempotencyKey));
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
                .addValue("currentSeq", nodeAccess.sequence()), (rs, _) -> new NextNode(
                UUID.fromString(rs.getString("id")),
                rs.getInt("numero_secuencia"),
                rs.getString("titulo"),
                rs.getString("estado")
        )).stream().findFirst();

        NextUnlockedNode nextUnlocked = null;

        if (nextNodeOpt.isPresent()) {
            var next = nextNodeOpt.get();
            if ("BLOQUEADO".equalsIgnoreCase(next.state())) {
                jdbc.update("""
                        UPDATE aprendizaje_progreso_nodo
                        SET estado = 'DISPONIBLE',
                            desbloqueado_en = CURRENT_TIMESTAMP(6)
                        WHERE inscripcion_id = :enrollment AND nodo_ruta_id = :nodeId
                        """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId())
                        .addValue("nodeId", next.id().toString()));
            }
            nextUnlocked = new NextUnlockedNode(next.id(), next.sequence(), next.title());
        } else {
            // Si no hay más nodos, la ruta completa queda como COMPLETADA
            jdbc.update("""
                    UPDATE aprendizaje_inscripcion_ruta
                    SET estado = 'COMPLETADO',
                        completado_en = CURRENT_TIMESTAMP(6)
                    WHERE id = :enrollment
                    """, new MapSqlParameterSource("enrollment", nodeAccess.enrollmentId()));
        }

        // 4. Calcular XP total acumulado del estudiante
        Integer totalXp = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :userId
                """, new MapSqlParameterSource("userId", studentId.toString()), Integer.class);

        // 5. Gestión de Insignias Escolares (HU-07)
        BadgeDto badgeEarned = null;
        if (!wasAlreadyCompleted) {
            String badgeId = "in000000-0000-4000-8000-000000000001";
            jdbc.update("""
                    INSERT IGNORE INTO gamificacion_insignia (id, codigo, nombre, descripcion, criterio, activa)
                    VALUES (:id, 'EXPLORADOR_AYACUCHO', 'Explorador de Ayacucho',
                            'Completaste tu primera parada escolar y superaste los desafíos de la lección.', '{}', TRUE)
                    """, new MapSqlParameterSource("id", badgeId));

            int inserted = jdbc.update("""
                    INSERT IGNORE INTO gamificacion_insignia_usuario (usuario_id, insignia_id, obtenida_en)
                    VALUES (:userId, :badgeId, CURRENT_TIMESTAMP(6))
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

        String message = wasAlreadyCompleted
                ? "¡Excelente repaso! Esta parada ya estaba completada en tu aventura."
                : (nextUnlocked != null
                ? "¡Felicitaciones! Has completado esta parada, sumaste " + experienceToAward + " XP y desbloqueaste la siguiente aventura con Rupi."
                : "¡Misión cumplida! Has completado todas las paradas de esta ruta de aprendizaje.");

        return new ActivityCompleteResponse(
                nodeId,
                "COMPLETADO",
                experienceToAward,
                totalXp != null ? totalXp : 0,
                nextUnlocked,
                message,
                badgeEarned
        );
    }
}
