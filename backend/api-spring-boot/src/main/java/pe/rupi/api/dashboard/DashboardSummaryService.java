package pe.rupi.api.dashboard;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import pe.rupi.api.dashboard.DashboardDtos.*;
import pe.rupi.api.gamification.RupiAccessoriesService;
import pe.rupi.api.gamification.StudyStreakService;
import pe.rupi.api.shared.StudentSessionService;

/**
 * Servicio de métricas y resumen diario del estudiante (HU-33).
 *
 * Cumple con AGENTS.md e invariantes arquitectónicas:
 * - Single writer: Solo lectura dinámica de aprendizaje_* y gamificacion_*.
 * - Los contadores y porcentajes se calculan en tiempo de consulta, sin columnas fijas duplicadas.
 * - Soporte para sincronización rápida y hashing para clientes móviles y web.
 */
@Service
public class DashboardSummaryService {
    private static final Logger log = LoggerFactory.getLogger(DashboardSummaryService.class);
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final NamedParameterJdbcTemplate jdbc;
    private final StudyStreakService streakService;
    private final RupiAccessoriesService accessoriesService;

    public DashboardSummaryService(
            NamedParameterJdbcTemplate jdbc,
            StudyStreakService streakService,
            RupiAccessoriesService accessoriesService
    ) {
        this.jdbc = jdbc;
        this.streakService = streakService;
        this.accessoriesService = accessoriesService;
    }

    public DashboardSummaryResponse getSummary(StudentSessionService.Student student) {
        String studentId = student.id();

        // 1. Accesorios equipados de Rupi
        List<String> equipped = Collections.emptyList();
        try {
            var equippedMap = accessoriesService.getEquippedMap(studentId);
            if (equippedMap != null && !equippedMap.isEmpty()) {
                equipped = new ArrayList<>(equippedMap.values());
            }
        } catch (Exception ex) {
            log.warn("No se pudieron cargar accesorios equipados de {}: {}", studentId, ex.getMessage());
        }

        StudentOverview overview = new StudentOverview(
                studentId,
                student.name(),
                student.schoolName(),
                student.role(),
                student.gradeNumber(),
                student.gradeName(),
                equipped
        );

        // 2. Progreso por materia / ruta inscrita
        List<SubjectProgress> subjects = loadSubjectProgress(studentId);

        // 3. Totales agregados de lecciones
        long totalCompleted = 0;
        long totalPending = 0;
        for (SubjectProgress sp : subjects) {
            totalCompleted += sp.completedLessons();
            totalPending += sp.pendingLessons();
        }
        long totalEnrolled = totalCompleted + totalPending;
        int overallPercentage = totalEnrolled > 0
                ? (int) Math.round((double) totalCompleted * 100.0 / totalEnrolled)
                : 0;

        // 4. Lecciones completadas hoy (Zona horaria de Perú)
        long completedToday = countLessonsCompletedToday(studentId);

        // 5. Racha de estudio y Puntos XP
        int streakDays = 0;
        try {
            var streak = streakService.current(studentId);
            if (streak != null) {
                streakDays = streak.days();
            }
        } catch (Exception ex) {
            log.warn("No se pudo calcular la racha de {}: {}", studentId, ex.getMessage());
        }

        int totalXp = calculateTotalXp(studentId);

        MetricSummary metrics = new MetricSummary(
                totalCompleted,
                totalPending,
                totalEnrolled,
                overallPercentage,
                completedToday,
                streakDays,
                totalXp,
                subjects.size()
        );

        // 6. Próxima lección recomendada para continuar de inmediato
        NextLessonRecommendation nextLesson = resolveNextLesson(studentId);

        // 7. Metadatos de sincronización rápida
        String syncedAt = Instant.now().toString();
        String dataHash = computeDataHash(metrics, nextLesson, subjects);
        SyncMeta sync = new SyncMeta(syncedAt, dataHash);

        return new DashboardSummaryResponse(overview, metrics, subjects, nextLesson, sync);
    }

    private List<SubjectProgress> loadSubjectProgress(String studentId) {
        String sql = """
                SELECT vr.id AS version_route_id,
                       r.titulo AS route_title,
                       a.codigo AS area_code,
                       a.nombre AS area_name,
                       g.numero_grado,
                       i.ultima_visita_en,
                       COUNT(DISTINCT n.id) AS total_nodes,
                       COUNT(DISTINCT CASE WHEN p.estado = 'COMPLETADO' THEN n.id END) AS completed_nodes
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN curriculo_grado g ON g.id = vr.grado_id
                JOIN curriculo_area a ON a.id = vr.area_id
                LEFT JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.version_ruta_id = vr.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :studentId
                  AND i.estado IN ('ACTIVO', 'COMPLETADO')
                  AND vr.estado = 'PUBLICADO'
                  AND r.archivado_en IS NULL
                GROUP BY vr.id, r.titulo, a.codigo, a.nombre, g.numero_grado, i.ultima_visita_en
                ORDER BY i.ultima_visita_en DESC, g.numero_grado, a.nombre
                """;

        var params = new MapSqlParameterSource("studentId", studentId);
        return jdbc.query(sql, params, (rs, rowNum) -> {
            long total = rs.getLong("total_nodes");
            long completed = rs.getLong("completed_nodes");
            long pending = Math.max(0, total - completed);
            int percentage = total > 0 ? (int) Math.round((double) completed * 100.0 / total) : 0;
            Timestamp lastVisit = rs.getTimestamp("ultima_visita_en");

            return new SubjectProgress(
                    UUID.fromString(rs.getString("version_route_id")),
                    rs.getString("route_title"),
                    rs.getString("area_code"),
                    rs.getString("area_name"),
                    rs.getInt("numero_grado"),
                    completed,
                    pending,
                    total,
                    percentage,
                    lastVisit != null ? lastVisit.toInstant().toString() : null
            );
        });
    }

    private long countLessonsCompletedToday(String studentId) {
        Instant todayStart = LocalDate.now(LIMA_ZONE).atStartOfDay(LIMA_ZONE).toInstant();
        String sql = """
                SELECT COUNT(DISTINCT p.nodo_ruta_id)
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_progreso_nodo p ON p.inscripcion_id = i.id
                WHERE i.estudiante_id = :studentId
                  AND p.estado = 'COMPLETADO'
                  AND p.completado_en >= :todayStart
                """;
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("todayStart", Timestamp.from(todayStart));
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    private int calculateTotalXp(String studentId) {
        String sql = """
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :studentId
                """;
        var params = new MapSqlParameterSource("studentId", studentId);
        Integer xp = jdbc.queryForObject(sql, params, Integer.class);
        return xp != null ? xp : 0;
    }

    private NextLessonRecommendation resolveNextLesson(String studentId) {
        String sql = """
                SELECT vr.id AS version_ruta_id,
                       r.titulo AS ruta_titulo,
                       a.codigo AS area_codigo,
                       a.nombre AS area_nombre,
                       n.id AS nodo_id,
                       n.numero_secuencia,
                       va.titulo AS nodo_titulo,
                       va.minutos_estimados,
                       act.tipo AS tipo_actividad,
                       CASE
                         WHEN va.estado <> 'PUBLICADO' OR act.archivado_en IS NOT NULL THEN 'BLOQUEADO'
                         ELSE COALESCE(p.estado, CASE WHEN n.numero_secuencia = 1 THEN 'DISPONIBLE' ELSE 'BLOQUEADO' END)
                       END AS estado_progreso
                FROM aprendizaje_inscripcion_ruta i
                JOIN aprendizaje_version_ruta vr ON vr.id = i.version_ruta_id
                JOIN aprendizaje_ruta r ON r.id = vr.ruta_id
                JOIN curriculo_area a ON a.id = vr.area_id
                JOIN aprendizaje_nodo_ruta n ON n.version_ruta_id = vr.id
                JOIN aprendizaje_version_actividad va ON va.id = n.version_actividad_id
                JOIN aprendizaje_actividad act ON act.id = va.actividad_id
                LEFT JOIN aprendizaje_progreso_nodo p
                       ON p.inscripcion_id = i.id
                      AND p.nodo_ruta_id = n.id
                WHERE i.estudiante_id = :studentId
                  AND i.estado = 'ACTIVO'
                  AND vr.estado = 'PUBLICADO'
                  AND r.archivado_en IS NULL
                HAVING estado_progreso IN ('EN_CURSO', 'DISPONIBLE')
                ORDER BY CASE WHEN estado_progreso = 'EN_CURSO' THEN 0 ELSE 1 END,
                         i.ultima_visita_en DESC,
                         n.numero_secuencia ASC
                LIMIT 1
                """;

        var params = new MapSqlParameterSource("studentId", studentId);
        List<NextLessonRecommendation> results = jdbc.query(sql, params, (rs, rowNum) -> {
            Integer minutes = rs.getObject("minutos_estimados", Integer.class);
            return new NextLessonRecommendation(
                    UUID.fromString(rs.getString("version_ruta_id")),
                    rs.getString("ruta_titulo"),
                    rs.getString("area_codigo"),
                    rs.getString("area_nombre"),
                    UUID.fromString(rs.getString("nodo_id")),
                    rs.getString("nodo_titulo"),
                    rs.getInt("numero_secuencia"),
                    rs.getString("tipo_actividad"),
                    minutes,
                    rs.getString("estado_progreso")
            );
        });

        return results.isEmpty() ? null : results.get(0);
    }

    private String computeDataHash(
            MetricSummary metrics,
            NextLessonRecommendation nextLesson,
            List<SubjectProgress> subjects
    ) {
        StringBuilder raw = new StringBuilder();
        raw.append(metrics.totalCompletedLessons()).append(':')
           .append(metrics.totalPendingLessons()).append(':')
           .append(metrics.lessonsCompletedToday()).append(':')
           .append(metrics.currentStreakDays()).append(':')
           .append(metrics.totalXp()).append(':')
           .append(nextLesson != null ? nextLesson.nodeId() : "none").append(':')
           .append(subjects.size());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.toString().hashCode());
        }
    }
}
