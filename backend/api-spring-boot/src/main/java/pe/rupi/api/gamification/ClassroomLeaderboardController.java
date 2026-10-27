package pe.rupi.api.gamification;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para el Leaderboard y dinámica de logros del aula escolar (HU-32).
 */
@RestController
@RequestMapping("/api/v1/student/leaderboard")
public class ClassroomLeaderboardController {

    private final ClassroomLeaderboardService leaderboardService;

    public ClassroomLeaderboardController(ClassroomLeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ClassroomLeaderboardService.ClassroomLeaderboard getLeaderboard(
            Principal principal,
            @RequestParam(name = "sort", defaultValue = "XP") String sort
    ) {
        String studentId = studentIdFrom(principal);
        return leaderboardService.getLeaderboard(studentId, sort);
    }

    @GetMapping("/privacy")
    public ClassroomLeaderboardService.PrivacySettings getPrivacy(Principal principal) {
        String studentId = studentIdFrom(principal);
        return leaderboardService.getPrivacy(studentId);
    }

    @PutMapping("/privacy")
    public ClassroomLeaderboardService.PrivacySettings updatePrivacy(
            Principal principal,
            @RequestBody ClassroomLeaderboardService.PrivacySettings update
    ) {
        String studentId = studentIdFrom(principal);
        return leaderboardService.updatePrivacy(studentId, update);
    }

    private static String studentIdFrom(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no autenticada.");
        }
        try {
            return UUID.fromString(principal.getName().trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identificador de estudiante inválido.");
        }
    }
}
