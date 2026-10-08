package pe.rupi.api.dailychallenge;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.dailychallenge.DailyChallengeDtos.DailyChallengeCompleteResponse;
import pe.rupi.api.dailychallenge.DailyChallengeDtos.DailyChallengeResponse;

@RestController
@RequestMapping("/api/v1/student/daily-challenge")
public class DailyChallengeController {
    private final DailyChallengeService service;

    public DailyChallengeController(DailyChallengeService service) {
        this.service = service;
    }

    @GetMapping
    public DailyChallengeResponse getTodayChallenge(Principal principal) {
        UUID studentId = studentIdFrom(principal);
        return service.getTodayChallenge(studentId);
    }

    @PostMapping("/{challengeId}/complete")
    public DailyChallengeCompleteResponse completeChallenge(
            @PathVariable UUID challengeId,
            Principal principal
    ) {
        UUID studentId = studentIdFrom(principal);
        return service.completeTodayChallenge(studentId, challengeId);
    }

    private static UUID studentIdFrom(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
