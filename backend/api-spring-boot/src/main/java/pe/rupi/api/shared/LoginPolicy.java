package pe.rupi.api.shared;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * HU-58: política de intentos de inicio de sesión que la consola guarda en identidad_politica_acceso.
 * <ul>
 *   <li>Límite por equipo: tras {@code perDeviceAccount} fallos de una cuenta desde el mismo equipo, o
 *       {@code perDevice} fallos desde ese equipo con cualquier cuenta, se espera {@code windowMinutes}.</li>
 *   <li>Bloqueo automático: si la cuenta suma {@code perAccount} fallos dentro de la ventana, venga de donde
 *       venga, queda bloqueada {@code lockMinutes} minutos.</li>
 * </ul>
 * El bloqueo no se escribe en identidad_cuenta_usuario (es de Django): se calcula con los intentos registrados.
 * Django hace el mismo cálculo en identidad/politica_acceso.py para la consola.
 */
public record LoginPolicy(int windowMinutes, int perDeviceAccount, int perDevice, boolean autoLock, int perAccount,
                          int lockMinutes) {

    /** Los valores que Spring y Django usaban fijos antes de la HU-58. */
    public static final LoginPolicy DEFAULT = new LoginPolicy(5, 5, 20, true, 10, 15);

    public Duration window() { return Duration.ofMinutes(windowMinutes); }

    public Duration lock() { return Duration.ofMinutes(lockMinutes); }

    /** Desde cuándo hay que leer fallos para saber si la cuenta sigue bloqueada. */
    public Instant lookback(Instant now) { return now.minus(window()).minus(lock()); }

    /**
     * Hasta cuándo queda bloqueada la cuenta. {@code failures} son los fallos de contraseña (INCORRECTO) desde
     * {@link #lookback}, ya sin los anteriores a un desbloqueo, en orden ascendente. Cada vez que {@code perAccount}
     * fallos caben en la ventana empieza un bloqueo; durante el bloqueo la contraseña no se prueba, así que esos
     * intentos se registran como BLOQUEADO y no lo alargan. Los fallos que ya causaron un bloqueo no vuelven a contar.
     */
    public Optional<Instant> lockedUntil(List<Instant> failures, Instant now) {
        if (!autoLock || failures.size() < perAccount) return Optional.empty();
        Instant until = null;
        int from = 0;
        for (int i = 0; i < failures.size(); i++) {
            Instant at = failures.get(i);
            if (until != null && !at.isAfter(until)) { from = i + 1; continue; }
            int first = i - perAccount + 1;
            if (first >= from && !at.isAfter(failures.get(first).plus(window()))) {
                until = at.plus(lock());
                from = i + 1;
            }
        }
        return until != null && until.isAfter(now) ? Optional.of(until) : Optional.empty();
    }

    /** Minutos que faltan, redondeados hacia arriba y nunca menos de uno, para el mensaje al estudiante. */
    public static int minutesLeft(Instant until, Instant now) {
        long seconds = Math.max(1, Duration.between(now, until).toSeconds());
        return (int) Math.max(1, (seconds + 59) / 60);
    }
}
