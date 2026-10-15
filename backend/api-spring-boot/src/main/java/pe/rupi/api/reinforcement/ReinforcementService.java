package pe.rupi.api.reinforcement;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.studypackage.PrimaryStudyCatalog;
import pe.rupi.api.studypackage.PrimaryStudyCatalog.Entry;
import pe.rupi.api.reinforcement.ReinforcementRepository.Evidence;

@Service
public class ReinforcementService {
    private final ReinforcementRepository repository;
    public ReinforcementService(ReinforcementRepository repository) { this.repository=repository; }
    public record Resource(String id, String title, String url, String publisher, boolean completed) {}
    public record View(String status, String nodeTitle, int answered, int correct, int threshold,
                       List<Evidence> practice, List<Resource> resources) {}
    static boolean needsPractice(List<Evidence> evidence) {
        return evidence.size()>=2 && evidence.stream().filter(Evidence::correct).count()*100 < evidence.size()*70L;
    }
    static String resourceId(Entry entry) {
        return UUID.nameUUIDFromBytes(entry.sourceUrl().getBytes(StandardCharsets.UTF_8)).toString();
    }
    @Transactional
    public View refresh(UUID student, UUID route, UUID node) {
        var context=repository.context(student,route,node).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var evidence=repository.evidence(student,context.activity());
        var catalog=PrimaryStudyCatalog.forGradeAndArea(context.grade(),context.area());
        boolean low=needsPractice(evidence);
        if(low) for(var entry:catalog) repository.link(student,node,resourceId(entry));
        var resources=new ArrayList<Resource>();
        for(var link:repository.links(student,node)) {
            catalog.stream().filter(e->resourceId(e).equals(link.resourceId())).findFirst().ifPresent(e->
                resources.add(new Resource(link.id(),e.title(),e.sourceUrl(),e.publisher(),link.completed())));
        }
        String status=evidence.size()<2 ? "SIN_EVIDENCIA" : low ? (catalog.isEmpty()?"SIN_RECURSO":"REFUERZO") : "AVANCE";
        return new View(status,context.title(),evidence.size(),(int)evidence.stream().filter(Evidence::correct).count(),70,
            evidence.stream().filter(e->!e.correct()).toList(),List.copyOf(resources));
    }
    @Transactional
    public void complete(UUID student, UUID route, UUID node, UUID id) {
        var view=refresh(student,route,node);
        if(view.resources().stream().noneMatch(r->r.id().equals(id.toString())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.complete(student,node,id);
    }
}
