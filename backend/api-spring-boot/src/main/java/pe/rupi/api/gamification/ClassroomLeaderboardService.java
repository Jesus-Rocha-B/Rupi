package pe.rupi.api.gamification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de tabla de posiciones (Leaderboard) y dinámica de logros a nivel de aula (HU-32).
 *
 * Cumple estrictamente con las reglas de AGENTS.md:
 * 1. Principio de escritor único: Spring Boot gestiona el cálculo de ranking y privacidad.
 * 2. Invariante de datos: El puntaje total de XP se deriva dinámicamente del libro mayor
 *    (gamificacion_movimiento_experiencia) y la racha de días de aprendizaje_progreso_nodo.
 * 3. Privacidad infantil: Respeto estricto a las opciones de visualización por nombre o seudónimo,
 *    y exclusión o modo incógnito para quienes opten por no participar públicamente.
 */
@Service
public class ClassroomLeaderboardService {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    public record EquippedSummary(
            String sombrero,
            String vestimenta,
            String fondo
    ) {}

    public record LeaderboardEntry(
            int rank,
            String studentId,
            String displayName,
            String aliasPublico,
            boolean esUsuarioActual,
            int totalXp,
            int streakDays,
            boolean streakActiveToday,
            EquippedSummary equipped
    ) {}

    public record PrivacySettings(
            boolean mostrarEnClasificacion,
            String aliasPublico,
            String mostrarComo // "NOMBRE" | "ALIAS"
    ) {}

    public record ClassroomLeaderboard(
            String classroomId,
            String classroomName,
            int gradeNumber,
            String sortBy, // "XP" | "RACHA"
            int myRank,
            int totalStudents,
            List<LeaderboardEntry> entries,
            PrivacySettings myPrivacy
    ) {}

    private final NamedParameterJdbcTemplate db;
    private final StudyStreakService streakService;

    public ClassroomLeaderboardService(NamedParameterJdbcTemplate db, StudyStreakService streakService) {
        this.db = db;
        this.streakService = streakService;
    }

    /**
     * Obtiene la tabla de posiciones del aula en la que está inscrito el estudiante.
     *
     * @param studentId ID del estudiante autenticado.
     * @param sortBy Criterio de ordenación: "XP" (por experiencia total) o "RACHA" (por constancia diaria).
     */
    public ClassroomLeaderboard getLeaderboard(String studentId, String sortBy) {
        final String effectiveSort = "RACHA".equalsIgnoreCase(sortBy) ? "RACHA" : "XP";

        // 1. Identificar el aula activa del estudiante
        var classroomParams = new MapSqlParameterSource("studentId", studentId);
        var classroomRows = db.query("""
                SELECT a.id AS aula_id, a.nombre AS aula_nombre, g.numero_grado
                FROM escuela_membresia_aula ma
                JOIN escuela_aula a ON a.id = ma.aula_id AND a.activa = TRUE
                JOIN curriculo_grado g ON g.id = a.grado_id
                WHERE ma.usuario_id = :studentId AND ma.estado = 'ACTIVO' AND ma.rol = 'ESTUDIANTE'
                ORDER BY ma.ingreso_en DESC
                LIMIT 1
                """, classroomParams, (rs, i) -> Map.of(
                "aulaId", rs.getString("aula_id"),
                "aulaNombre", rs.getString("aula_nombre"),
                "grado", rs.getInt("numero_grado")
        ));

        if (classroomRows.isEmpty()) {
            // Estudiante sin aula formal activa: devolver vista individual amigable
            var selfPrivacy = getPrivacy(studentId);
            return new ClassroomLeaderboard(
                    "",
                    "Mi Espacio de Aprendizaje",
                    0,
                    effectiveSort,
                    1,
                    1,
                    List.of(new LeaderboardEntry(
                            1,
                            studentId,
                            selfPrivacy.mostrarComo().equals("NOMBRE") ? "Tú" : (selfPrivacy.aliasPublico() != null ? selfPrivacy.aliasPublico() : "Tú"),
                            selfPrivacy.aliasPublico(),
                            true,
                            calculateStudentXp(studentId),
                            streakService.current(studentId).days(),
                            streakService.current(studentId).activeToday(),
                            new EquippedSummary(null, null, null)
                    )),
                    selfPrivacy
            );
        }

        var classroom = classroomRows.get(0);
        String classroomId = (String) classroom.get("aulaId");
        String classroomName = (String) classroom.get("aulaNombre");
        int gradeNumber = (int) classroom.get("grado");

        var myPrivacy = getPrivacy(studentId);

        // 2. Obtener todos los compañeros matriculados en esa misma aula
        var membersParams = new MapSqlParameterSource("classroomId", classroomId);
        var rawMembers = db.query("""
                SELECT 
                    u.id AS usuario_id,
                    u.nombre_usuario,
                    p.nombre,
                    p.apellido,
                    p.nombre_preferido,
                    p.alias_publico,
                    COALESCE(p.mostrar_como, 'ALIAS') AS mostrar_como,
                    COALESCE(p.mostrar_en_clasificacion, 1) AS mostrar_en_clasificacion,
                    COALESCE((
                        SELECT SUM(xp.cantidad) 
                        FROM gamificacion_movimiento_experiencia xp 
                        WHERE xp.usuario_id = u.id
                    ), 0) AS total_xp
                FROM escuela_membresia_aula ma
                JOIN identidad_cuenta_usuario u ON u.id = ma.usuario_id
                LEFT JOIN identidad_perfil_usuario p ON p.usuario_id = u.id
                WHERE ma.aula_id = :classroomId AND ma.estado = 'ACTIVO' AND ma.rol = 'ESTUDIANTE'
                """, membersParams, (rs, i) -> new RawMember(
                rs.getString("usuario_id"),
                rs.getString("nombre_usuario"),
                rs.getString("nombre"),
                rs.getString("nombre_preferido"),
                rs.getString("alias_publico"),
                rs.getString("mostrar_como"),
                rs.getInt("mostrar_en_clasificacion") == 1,
                rs.getInt("total_xp")
        ));

        if (rawMembers.isEmpty()) {
            return new ClassroomLeaderboard(classroomId, classroomName, gradeNumber, effectiveSort, 1, 0, List.of(), myPrivacy);
        }

        // 3. Consultar accesorios equipados de todos los miembros del aula en una sola consulta batch
        List<String> memberIds = rawMembers.stream().map(RawMember::usuarioId).toList();
        Map<String, Map<String, String>> accessoriesByMember = fetchEquippedAccessoriesBatch(memberIds);

        // 4. Transformar a DTO respetando la privacidad de cada estudiante
        List<PreEntry> preEntries = new ArrayList<>();
        for (RawMember m : rawMembers) {
            boolean isSelf = m.usuarioId().equals(studentId);

            // Si otro alumno decidió no mostrarse en la clasificación, se omite de la lista pública
            if (!m.mostrarEnClasificacion() && !isSelf) {
                continue;
            }

            // Resolver nombre visible según preferencia de privacidad
            String resolvedDisplayName;
            if (isSelf) {
                // Al usuario actual siempre se le muestra su nombre o alias según su configuración
                if ("NOMBRE".equalsIgnoreCase(m.mostrarComo()) && (m.nombrePreferido() != null || m.nombre() != null)) {
                    resolvedDisplayName = (m.nombrePreferido() != null ? m.nombrePreferido() : m.nombre()) + " (Tú)";
                } else if (m.aliasPublico() != null && !m.aliasPublico().isBlank()) {
                    resolvedDisplayName = m.aliasPublico() + " (Tú)";
                } else {
                    resolvedDisplayName = "Tú";
                }
            } else {
                if ("NOMBRE".equalsIgnoreCase(m.mostrarComo()) && (m.nombrePreferido() != null || m.nombre() != null)) {
                    resolvedDisplayName = m.nombrePreferido() != null ? m.nombrePreferido() : m.nombre();
                } else if (m.aliasPublico() != null && !m.aliasPublico().isBlank()) {
                    resolvedDisplayName = m.aliasPublico();
                } else {
                    // Respaldo de privacidad para menores: apodo escolar aleatorio o inicial
                    resolvedDisplayName = "Compañero " + m.usuarioId().substring(0, 4);
                }
            }

            // Calcular racha en tiempo de consulta
            var streak = streakService.current(m.usuarioId());

            Map<String, String> userAccessories = accessoriesByMember.getOrDefault(m.usuarioId(), Collections.emptyMap());
            EquippedSummary equipped = new EquippedSummary(
                    userAccessories.get("SOMBRERO"),
                    userAccessories.get("VESTIMENTA"),
                    userAccessories.get("FONDO")
            );

            preEntries.add(new PreEntry(
                    m.usuarioId(),
                    resolvedDisplayName,
                    m.aliasPublico(),
                    isSelf,
                    m.totalXp(),
                    streak.days(),
                    streak.activeToday(),
                    equipped
            ));
        }

        // 5. Ordenar según el criterio seleccionado (Descendente por puntaje o racha)
        if ("RACHA".equals(effectiveSort)) {
            preEntries.sort((a, b) -> {
                int cmp = Integer.compare(b.streakDays(), a.streakDays());
                if (cmp != 0) return cmp;
                cmp = Integer.compare(b.totalXp(), a.totalXp());
                if (cmp != 0) return cmp;
                return a.displayName().compareToIgnoreCase(b.displayName());
            });
        } else {
            preEntries.sort((a, b) -> {
                int cmp = Integer.compare(b.totalXp(), a.totalXp());
                if (cmp != 0) return cmp;
                cmp = Integer.compare(b.streakDays(), a.streakDays());
                if (cmp != 0) return cmp;
                return a.displayName().compareToIgnoreCase(b.displayName());
            });
        }

        // 6. Asignar puestos correlativos (1.°, 2.°, 3.°...) y localizar la posición del usuario
        List<LeaderboardEntry> finalEntries = new ArrayList<>();
        int myRank = 0;
        for (int i = 0; i < preEntries.size(); i++) {
            int rank = i + 1;
            PreEntry pe = preEntries.get(i);
            if (pe.isSelf()) {
                myRank = rank;
            }
            finalEntries.add(new LeaderboardEntry(
                    rank,
                    pe.studentId(),
                    pe.displayName(),
                    pe.aliasPublico(),
                    pe.isSelf(),
                    pe.totalXp(),
                    pe.streakDays(),
                    pe.streakActiveToday(),
                    pe.equipped()
            ));
        }

        return new ClassroomLeaderboard(
                classroomId,
                classroomName,
                gradeNumber,
                effectiveSort,
                myRank,
                finalEntries.size(),
                finalEntries,
                myPrivacy
        );
    }

    /**
     * Obtiene la configuración de privacidad de un estudiante.
     */
    public PrivacySettings getPrivacy(String studentId) {
        var params = new MapSqlParameterSource("studentId", studentId);
        var rows = db.query("""
                SELECT alias_publico, COALESCE(mostrar_como, 'ALIAS') AS mostrar_como, COALESCE(mostrar_en_clasificacion, 1) AS mostrar_en_clasificacion
                FROM identidad_perfil_usuario
                WHERE usuario_id = :studentId
                """, params, (rs, i) -> new PrivacySettings(
                rs.getInt("mostrar_en_clasificacion") == 1,
                rs.getString("alias_publico"),
                rs.getString("mostrar_como")
        ));

        if (rows.isEmpty()) {
            return new PrivacySettings(true, null, "ALIAS");
        }
        return rows.get(0);
    }

    /**
     * Actualiza la configuración de privacidad y el seudónimo público del estudiante.
     */
    @Transactional
    public PrivacySettings updatePrivacy(String studentId, PrivacySettings update) {
        if (update == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de privacidad requeridos.");
        }

        String alias = update.aliasPublico() != null ? update.aliasPublico().trim() : null;
        if (alias != null && !alias.isBlank()) {
            if (!ALIAS_PATTERN.matcher(alias).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El seudónimo debe tener entre 3 y 30 caracteres alfanuméricos o guion bajo.");
            }

            // Validar unicidad de seudónimo en la plataforma
            var checkParams = new MapSqlParameterSource("alias", alias).addValue("studentId", studentId);
            Integer taken = db.queryForObject("""
                    SELECT COUNT(*) 
                    FROM identidad_perfil_usuario 
                    WHERE LOWER(alias_publico) = LOWER(:alias) AND usuario_id <> :studentId
                    """, checkParams, Integer.class);
            if (taken != null && taken > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este seudónimo ya está siendo usado por otro compañero. Elige uno diferente.");
            }
        }

        String como = "NOMBRE".equalsIgnoreCase(update.mostrarComo()) ? "NOMBRE" : "ALIAS";
        boolean mostrar = update.mostrarEnClasificacion();

        // En caso de querer mostrarse, si no tiene alias se valida que el alias no sea nulo (por constraint de BD)
        if (mostrar && (alias == null || alias.isBlank())) {
            // Si el perfil ya tiene un alias guardado, conservarlo
            var current = getPrivacy(studentId);
            if (current.aliasPublico() != null && !current.aliasPublico().isBlank()) {
                alias = current.aliasPublico();
            } else {
                // Generar seudónimo por defecto si no tenía
                alias = "estudiante_" + studentId.substring(0, 5);
            }
        }

        var updateParams = new MapSqlParameterSource("studentId", studentId)
                .addValue("alias", alias)
                .addValue("mostrarComo", como)
                .addValue("mostrar", mostrar ? 1 : 0);

        int affected = db.update("""
                UPDATE identidad_perfil_usuario
                SET alias_publico = :alias,
                    mostrar_como = :mostrarComo,
                    mostrar_en_clasificacion = :mostrar
                WHERE usuario_id = :studentId
                """, updateParams);

        if (affected == 0) {
            // Si no existía perfil, crearlo
            db.update("""
                    INSERT INTO identidad_perfil_usuario 
                        (usuario_id, alias_publico, mostrar_como, mostrar_en_clasificacion, idioma, zona_horaria)
                    VALUES 
                        (:studentId, :alias, :mostrarComo, :mostrar, 'es-PE', 'America/Lima')
                    ON DUPLICATE KEY UPDATE 
                        alias_publico = VALUES(alias_publico),
                        mostrar_como = VALUES(mostrar_como),
                        mostrar_en_clasificacion = VALUES(mostrar_en_clasificacion)
                    """, updateParams);
        }

        return new PrivacySettings(mostrar, alias, como);
    }

    private int calculateStudentXp(String studentId) {
        var params = new MapSqlParameterSource("studentId", studentId);
        Integer total = db.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0) 
                FROM gamificacion_movimiento_experiencia 
                WHERE usuario_id = :studentId
                """, params, Integer.class);
        return total != null ? total : 0;
    }

    private Map<String, Map<String, String>> fetchEquippedAccessoriesBatch(List<String> memberIds) {
        if (memberIds.isEmpty()) return Collections.emptyMap();

        var params = new MapSqlParameterSource("memberIds", memberIds);
        var rows = db.query("""
                SELECT i.usuario_id, a.tipo, a.codigo
                FROM gamificacion_inventario_usuario i
                JOIN gamificacion_accesorio a ON a.id = i.accesorio_id
                WHERE i.equipado = TRUE AND i.usuario_id IN (:memberIds)
                """, params, (rs, i) -> Map.of(
                "userId", rs.getString("usuario_id"),
                "tipo", rs.getString("tipo"),
                "codigo", rs.getString("codigo")
        ));

        Map<String, Map<String, String>> result = new HashMap<>();
        for (var row : rows) {
            String userId = row.get("userId");
            String tipo = row.get("tipo");
            String codigo = row.get("codigo");
            result.computeIfAbsent(userId, k -> new HashMap<>()).put(tipo, codigo);
        }
        return result;
    }

    private record RawMember(
            String usuarioId,
            String nombreUsuario,
            String nombre,
            String nombrePreferido,
            String aliasPublico,
            String mostrarComo,
            boolean mostrarEnClasificacion,
            int totalXp
    ) {}

    private record PreEntry(
            String studentId,
            String displayName,
            String aliasPublico,
            boolean isSelf,
            int totalXp,
            int streakDays,
            boolean streakActiveToday,
            EquippedSummary equipped
    ) {}
}
