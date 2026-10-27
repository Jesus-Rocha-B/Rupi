package pe.rupi.api.gamification;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/streak")
public class StudyStreakController {
    private final StudyStreakService streaks;

    public StudyStreakController(StudyStreakService streaks) {
        this.streaks = streaks;
    }

    @GetMapping
    public StudyStreakService.Streak current(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return streaks.current(UUID.fromString(principal.getName()).toString());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
