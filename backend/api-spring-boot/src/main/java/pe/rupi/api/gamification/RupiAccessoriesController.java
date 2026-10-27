package pe.rupi.api.gamification;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student/accessories")
public class RupiAccessoriesController {

    private final RupiAccessoriesService service;

    public RupiAccessoriesController(RupiAccessoriesService service) {
        this.service = service;
    }

    @GetMapping
    public RupiAccessoriesService.StudentWardrobe getWardrobe(Principal principal) {
        String studentId = studentIdFrom(principal);
        return service.getWardrobe(studentId);
    }

    @PostMapping("/{codigo}/unlock")
    public RupiAccessoriesService.AccessoryItem unlockAccessory(
            Principal principal,
            @PathVariable("codigo") String codigo
    ) {
        String studentId = studentIdFrom(principal);
        return service.unlockAccessory(studentId, codigo);
    }

    @PutMapping("/{codigo}/equip")
    public List<RupiAccessoriesService.AccessoryItem> equipAccessory(
            Principal principal,
            @PathVariable("codigo") String codigo,
            @RequestParam(name = "equip", defaultValue = "true") boolean equip
    ) {
        String studentId = studentIdFrom(principal);
        return service.equipAccessory(studentId, codigo, equip);
    }

    @GetMapping("/equipped")
    public Map<String, String> getEquipped(Principal principal) {
        String studentId = studentIdFrom(principal);
        return service.getEquippedMap(studentId);
    }

    private static String studentIdFrom(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(principal.getName()).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
