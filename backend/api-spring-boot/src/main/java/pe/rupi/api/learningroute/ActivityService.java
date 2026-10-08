package pe.rupi.api.learningroute;

<<<<<<< HEAD
import java.util.List;
import java.util.UUID;
=======
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
<<<<<<< HEAD
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivityService {
    private final NamedParameterJdbcTemplate jdbc;
    public ActivityService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
=======
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.evaluation.EvaluationService;
import pe.rupi.api.evaluation.EvaluationDtos.QuestionDto;
import pe.rupi.api.learningroute.LearningRouteDtos.ActivityCompleteResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.ContentBlock;
import pe.rupi.api.learningroute.LearningRouteDtos.NextUnlockedNode;

@Service
public class ActivityService {
    private final NamedParameterJdbcTemplate jdbc;
    private final EvaluationService evaluationService;

    public ActivityService(NamedParameterJdbcTemplate jdbc, EvaluationService evaluationService) {
        this.jdbc = jdbc;
        this.evaluationService = evaluationService;
    }

>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    public record Block(String id, String type, String text, String url, String accessibleText) {}
    public record Activity(
            String nodeId,
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
<<<<<<< HEAD
                .addValue("route", routeId.toString()).addValue("node", nodeId.toString());
        // Lock the enrollment and progress together. Repeated starts preserve first access and completion.
        var access = jdbc.query("""
                SELECT i.id AS enrollment_id, va.id AS activity_id, va.titulo, va.instrucciones, p.estado
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id AND vr.estado = 'PUBLICADO'
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id AND r.archivado_en IS NULL
                JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id AND n.id = :node
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id AND va.estado = 'PUBLICADO'
                JOIN aprendizaje_actividad a ON a.id = va.actividad_id AND a.archivado_en IS NULL
                JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id = i.id
                    AND p.version_ruta_id = vr.id AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :student AND i.version_ruta_id = :route
                    AND i.estado IN ('ACTIVO','COMPLETADO')
                FOR UPDATE OF i, p
                """, params, (rs, row) -> new Access(rs.getString("enrollment_id"), rs.getString("activity_id"),
                    rs.getString("titulo"), rs.getString("instrucciones"), rs.getString("estado")));
        if (access.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var item = access.getFirst();
        if (!List.of("DISPONIBLE", "EN_CURSO", "COMPLETADO").contains(item.state()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        params.addValue("activity", item.activityId()).addValue("enrollment", item.enrollmentId());
        var blocks = jdbc.query("""
                SELECT b.id, b.tipo, b.texto_cuerpo, b.texto_accesibilidad, m.url_externa
                FROM aprendizaje_bloque_contenido b
                LEFT JOIN recurso_archivo_multimedia m ON m.id = b.archivo_multimedia_id
                WHERE b.version_actividad_id = :activity ORDER BY b.numero_secuencia, b.id
                """, params, (rs, row) -> new Block(rs.getString("id"), rs.getString("tipo"),
                    rs.getString("texto_cuerpo"), rs.getString("url_externa"), rs.getString("texto_accesibilidad")));
        if (blocks.isEmpty() && (item.instructions() == null || item.instructions().isBlank()))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        jdbc.update("""
                UPDATE aprendizaje_progreso_nodo SET estado = CASE WHEN estado = 'DISPONIBLE' THEN 'EN_CURSO' ELSE estado END,
                    primer_acceso_en = COALESCE(primer_acceso_en, UTC_TIMESTAMP(6))
                WHERE inscripcion_id = :enrollment AND version_ruta_id = :route AND nodo_ruta_id = :node
                """, params);
        jdbc.update("""
                UPDATE aprendizaje_inscripcion_ruta SET ultimo_nodo_visitado_id = :node, ultima_visita_en = UTC_TIMESTAMP(6)
                WHERE id = :enrollment AND estudiante_id = :student AND version_ruta_id = :route
                """, params);
        return new Activity(nodeId.toString(), item.title(), item.instructions(),
                "DISPONIBLE".equals(item.state()) ? "EN_CURSO" : item.state(), blocks);
=======
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
        return new Activity(nodeId.toString(), access.title(), access.instructions(), currentState, blocks, questions);
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
                message
        );
>>>>>>> 656170f (Se implementó el flujo completo de inicio y finalización de lecciones escolares con otorgamiento idempotente de 50 puntos de experiencia y desbloqueo automático de la siguiente parada para cumplir con HU-02 y HU-05, además se configuró el desplazamiento suave y centrado accesible del mapa en el último nodo visitado junto con el acceso directo desde el botón de bienvenida para cumplir con HU-03, también se corrigieron las discrepancias de columnas de catálogo en el repositorio de Spring Boot para enlazar las unidades y competencias curriculares oficiales del MINEDU con sus respectivas bandas visuales en el roadmap para cumplir con HU-04, asimismo se diseñó el modal didáctico de actividades con estados de carga interactivos, contenido explicativo de respaldo y pantalla de celebración con Rupi festejando y audio de felicitación, y finalmente se agregaron las pruebas unitarias en JUnit, el script de integración para PowerShell y la documentación técnica detallada de las cuatro historias en la carpeta de implementación)
    }
}
