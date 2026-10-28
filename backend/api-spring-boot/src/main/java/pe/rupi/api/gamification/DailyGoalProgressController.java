package pe.rupi.api.gamification;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.gamification.DailyGoalDtos.DailyGoalOverviewResponse;
import pe.rupi.api.gamification.DailyGoalDtos.ScoreComparisonResponse;
import pe.rupi.api.gamification.DailyGoalDtos.UpdateTargetGoalRequest;
import pe.rupi.api.shared.StudentSessionService;

/**
 * Controlador de meta diaria y progreso gráfico del estudiante (HU-34).
 */
@RestController
@RequestMapping("/api/v1/student/daily-goal")
public class DailyGoalProgressController {
    private final DailyGoalProgressService goalService;

    public DailyGoalProgressController(DailyGoalProgressService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public DailyGoalOverviewResponse getDailyGoal(HttpServletRequest request) {
        StudentSessionService.Student student = resolveStudent(request);
        return goalService.getDailyGoalOverview(student.id());
    }

    /** Comparación del puntaje de hoy con el de ayer (HU-36). */
    @GetMapping("/comparison")
    public ScoreComparisonResponse getScoreComparison(HttpServletRequest request) {
        StudentSessionService.Student student = resolveStudent(request);
        return goalService.getScoreComparison(student.id());
    }

    @PutMapping("/target")
    public DailyGoalOverviewResponse updateTarget(
            @RequestParam(value = "target", required = false) Integer targetParam,
            @RequestBody(required = false) UpdateTargetGoalRequest body,
            HttpServletRequest request
    ) {
        StudentSessionService.Student student = resolveStudent(request);
        int target = targetParam != null ? targetParam : (body != null ? body.targetLessons() : 0);
        if (target <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar una meta de lecciones válida.");
        }
        return goalService.updateTargetGoal(student.id(), target);
    }

    private StudentSessionService.Student resolveStudent(HttpServletRequest request) {
        StudentSessionService.Student student = (StudentSessionService.Student) request.getAttribute("student");
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return student;
    }
}
