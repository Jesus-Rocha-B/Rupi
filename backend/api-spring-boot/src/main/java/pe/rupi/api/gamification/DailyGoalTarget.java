package pe.rupi.api.gamification;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Una sola meta diaria por estudiante para el anillo (HU-34, HU-35) y para los avisos (HU-25).
 *
 * Antes había dos: «Mi meta de hoy» la guardaba en identidad_perfil_usuario.meta_diaria_actividades, una tabla de
 * Django que Spring no puede escribir (respondía 503), y «Mi ritmo de estudio» en gamificacion_preferencia_estudio,
 * así que el alumno veía 3 en un lado y 1 en el otro. La fuente de verdad es gamificacion_preferencia_estudio; la
 * columna antigua solo se lee mientras el alumno no haya guardado su meta en ninguna de las dos pantallas.
 */
public final class DailyGoalTarget {
    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int DEFAULT = 3;
    public static final List<Integer> OPTIONS = List.of(1, 2, 3, 4, 5);

    private DailyGoalTarget() {
    }

    public static int resolve(NamedParameterJdbcTemplate db, String studentId) {
        var params = new MapSqlParameterSource("studentId", studentId);
        List<Integer> saved = db.queryForList("""
                SELECT objetivo_actividades FROM gamificacion_preferencia_estudio WHERE usuario_id = :studentId
                """, params, Integer.class);
        if (!saved.isEmpty() && saved.get(0) != null) {
            return clamp(saved.get(0));
        }
        try {
            List<Integer> legacy = db.queryForList("""
                    SELECT meta_diaria_actividades FROM identidad_perfil_usuario WHERE usuario_id = :studentId
                    """, params, Integer.class);
            if (!legacy.isEmpty() && legacy.get(0) != null) {
                return clamp(legacy.get(0));
            }
        } catch (DataAccessException ignored) {
            // Base sin V29: no existe la columna antigua y se usa la meta por defecto.
        }
        return DEFAULT;
    }

    /** Guarda la meta y sube la revisión, así «Mi ritmo de estudio» abierto en otro dispositivo pide recargar. */
    public static void save(NamedParameterJdbcTemplate db, String studentId, int target, LocalDate today) {
        var params = new MapSqlParameterSource()
                .addValue("studentId", studentId)
                .addValue("goal", target)
                .addValue("date", today);
        db.update("""
                INSERT INTO gamificacion_preferencia_estudio (usuario_id, objetivo_actividades, revision, actualizada_en)
                VALUES (:studentId, :goal, 1, UTC_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE objetivo_actividades = :goal, revision = revision + 1, actualizada_en = UTC_TIMESTAMP(6)
                """, params);
        db.update("""
                INSERT INTO gamificacion_meta_diaria (usuario_id, fecha_local, tipo, objetivo)
                VALUES (:studentId, :date, 'ACTIVIDADES', :goal)
                ON DUPLICATE KEY UPDATE objetivo = :goal
                """, params);
    }

    static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
