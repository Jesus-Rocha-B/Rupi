package pe.rupi.api.gamification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RupiAccessoriesService {

    public record AccessoryItem(
            String id,
            String codigo,
            String nombre,
            String descripcion,
            String tipo,
            int costoExperiencia,
            boolean desbloqueado,
            boolean equipado
    ) {}

    public record StudentWardrobe(
            int totalXp,
            List<AccessoryItem> items
    ) {}

    private final NamedParameterJdbcTemplate jdbc;

    public RupiAccessoriesService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int getStudentTotalXp(String studentId) {
        var params = new MapSqlParameterSource("userId", studentId);
        Integer total = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM gamificacion_movimiento_experiencia
                WHERE usuario_id = :userId
                """, params, Integer.class);
        return Math.max(0, total == null ? 0 : total);
    }

    @Transactional
    public StudentWardrobe getWardrobe(String studentId) {
        int totalXp = getStudentTotalXp(studentId);

        // Auto-otorgar accesorios iniciales gratuitos (costo_experiencia = 0)
        autoGrantFreeAccessories(studentId);

        String sql = """
                SELECT a.id, a.codigo, a.nombre, a.descripcion, a.tipo, a.costo_experiencia,
                       CASE WHEN inv.usuario_id IS NOT NULL THEN 1 ELSE 0 END AS desbloqueado,
                       COALESCE(inv.equipado, 0) AS equipado
                FROM gamificacion_accesorio a
                LEFT JOIN gamificacion_inventario_usuario inv
                  ON inv.accesorio_id = a.id AND inv.usuario_id = :userId
                WHERE a.activo = 1
                ORDER BY a.tipo, a.costo_experiencia ASC, a.nombre ASC
                """;

        List<AccessoryItem> items = jdbc.query(sql, new MapSqlParameterSource("userId", studentId), this::mapAccessory);
        return new StudentWardrobe(totalXp, items);
    }

    @Transactional
    public AccessoryItem unlockAccessory(String studentId, String codigo) {
        var params = new MapSqlParameterSource("codigo", codigo);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, codigo, nombre, descripcion, tipo, costo_experiencia
                FROM gamificacion_accesorio
                WHERE codigo = :codigo AND activo = 1
                """, params);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Accesorio no encontrado: " + codigo);
        }

        var acc = rows.getFirst();
        String accId = (String) acc.get("id");
        int cost = ((Number) acc.get("costo_experiencia")).intValue();

        // Verificar si ya está desbloqueado
        var checkParams = new MapSqlParameterSource("userId", studentId).addValue("accId", accId);
        Integer exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM gamificacion_inventario_usuario
                WHERE usuario_id = :userId AND accesorio_id = :accId
                """, checkParams, Integer.class);

        if (exists != null && exists > 0) {
            // Ya lo tiene en el inventario
            boolean equipped = isEquipped(studentId, accId);
            return new AccessoryItem(
                    accId,
                    (String) acc.get("codigo"),
                    (String) acc.get("nombre"),
                    (String) acc.get("descripcion"),
                    (String) acc.get("tipo"),
                    cost,
                    true,
                    equipped
            );
        }

        // Verificar saldo de XP
        int totalXp = getStudentTotalXp(studentId);
        if (totalXp < cost) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Necesitas " + cost + " XP para desbloquear este accesorio. Tienes " + totalXp + " XP.");
        }

        // Registrar en inventario
        jdbc.update("""
                INSERT INTO gamificacion_inventario_usuario (usuario_id, accesorio_id, equipado)
                VALUES (:userId, :accId, 0)
                """, checkParams);

        return new AccessoryItem(
                accId,
                (String) acc.get("codigo"),
                (String) acc.get("nombre"),
                (String) acc.get("descripcion"),
                (String) acc.get("tipo"),
                cost,
                true,
                false
        );
    }

    @Transactional
    public List<AccessoryItem> equipAccessory(String studentId, String codigo, boolean equip) {
        var params = new MapSqlParameterSource("userId", studentId).addValue("codigo", codigo);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id, a.tipo, inv.usuario_id
                FROM gamificacion_accesorio a
                LEFT JOIN gamificacion_inventario_usuario inv
                  ON inv.accesorio_id = a.id AND inv.usuario_id = :userId
                WHERE a.codigo = :codigo AND a.activo = 1
                """, params);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Accesorio no encontrado: " + codigo);
        }

        var row = rows.getFirst();
        if (row.get("usuario_id") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes desbloquear el accesorio antes de equiparlo.");
        }

        String accId = (String) row.get("id");
        String tipo = (String) row.get("tipo");

        if (equip) {
            // Desequipar cualquier otro accesorio del mismo tipo
            var unequipParams = new MapSqlParameterSource("userId", studentId).addValue("tipo", tipo);
            jdbc.update("""
                    UPDATE gamificacion_inventario_usuario inv
                    JOIN gamificacion_accesorio a ON a.id = inv.accesorio_id
                    SET inv.equipado = 0
                    WHERE inv.usuario_id = :userId AND a.tipo = :tipo
                    """, unequipParams);

            // Equipar el seleccionado
            var equipParams = new MapSqlParameterSource("userId", studentId).addValue("accId", accId);
            jdbc.update("""
                    UPDATE gamificacion_inventario_usuario
                    SET equipado = 1
                    WHERE usuario_id = :userId AND accesorio_id = :accId
                    """, equipParams);
        } else {
            // Desequipar
            var unequipParams = new MapSqlParameterSource("userId", studentId).addValue("accId", accId);
            jdbc.update("""
                    UPDATE gamificacion_inventario_usuario
                    SET equipado = 0
                    WHERE usuario_id = :userId AND accesorio_id = :accId
                    """, unequipParams);
        }

        return getEquippedList(studentId);
    }

    public Map<String, String> getEquippedMap(String studentId) {
        String sql = """
                SELECT a.tipo, a.codigo
                FROM gamificacion_inventario_usuario inv
                JOIN gamificacion_accesorio a ON a.id = inv.accesorio_id
                WHERE inv.usuario_id = :userId AND inv.equipado = 1
                """;

        Map<String, String> result = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("userId", studentId), rs -> {
            result.put(rs.getString("tipo"), rs.getString("codigo"));
        });
        return result;
    }

    private List<AccessoryItem> getEquippedList(String studentId) {
        String sql = """
                SELECT a.id, a.codigo, a.nombre, a.descripcion, a.tipo, a.costo_experiencia,
                       1 AS desbloqueado, 1 AS equipado
                FROM gamificacion_inventario_usuario inv
                JOIN gamificacion_accesorio a ON a.id = inv.accesorio_id
                WHERE inv.usuario_id = :userId AND inv.equipado = 1
                ORDER BY a.tipo
                """;
        return jdbc.query(sql, new MapSqlParameterSource("userId", studentId), this::mapAccessory);
    }

    private boolean isEquipped(String studentId, String accId) {
        var params = new MapSqlParameterSource("userId", studentId).addValue("accId", accId);
        Integer eq = jdbc.queryForObject("""
                SELECT COUNT(*) FROM gamificacion_inventario_usuario
                WHERE usuario_id = :userId AND accesorio_id = :accId AND equipado = 1
                """, params, Integer.class);
        return eq != null && eq > 0;
    }

    private void autoGrantFreeAccessories(String studentId) {
        var params = new MapSqlParameterSource("userId", studentId);
        jdbc.update("""
                INSERT IGNORE INTO gamificacion_inventario_usuario (usuario_id, accesorio_id, equipado)
                SELECT :userId, a.id, 0
                FROM gamificacion_accesorio a
                WHERE a.costo_experiencia = 0 AND a.activo = 1
                """, params);
    }

    private AccessoryItem mapAccessory(ResultSet rs, int rowNum) throws SQLException {
        return new AccessoryItem(
                rs.getString("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("tipo"),
                rs.getInt("costo_experiencia"),
                rs.getInt("desbloqueado") == 1,
                rs.getInt("equipado") == 1
        );
    }
}
