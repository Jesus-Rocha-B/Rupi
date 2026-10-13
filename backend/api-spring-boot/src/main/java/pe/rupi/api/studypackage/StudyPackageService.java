package pe.rupi.api.studypackage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pe.rupi.api.learningroute.LearningRouteDtos.Competency;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteDetailResponse;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteNode;
import pe.rupi.api.learningroute.LearningRouteDtos.RouteUnit;
import pe.rupi.api.learningroute.LearningRouteService;
import pe.rupi.api.material.MaterialStorageService;
import pe.rupi.api.studypackage.StudyPackageModel.Block;
import pe.rupi.api.studypackage.StudyPackageModel.FileRef;
import pe.rupi.api.studypackage.StudyPackageModel.Resource;
import pe.rupi.api.studypackage.StudyPackageModel.Stop;
import pe.rupi.api.studypackage.StudyPackageModel.Unit;

@Service
public class StudyPackageService {
    /** Clave de la URL para las paradas que no tienen unidad MINEDU (grupo «Otras paradas»). */
    public static final String LOOSE_UNIT_KEY = "otras";

    private final LearningRouteService routes;
    private final StudyPackageRepository repository;
    private final MaterialStorageService materials;
    private final StudyPackageCache cache;
    private final StudyPackageBuilder builder;
    private final Clock clock;

    @Autowired
    public StudyPackageService(LearningRouteService routes, StudyPackageRepository repository,
                               MaterialStorageService materials, StudyPackageCache cache) {
        this(routes, repository, materials, cache, new StudyPackageBuilder(), Clock.systemUTC());
    }

    StudyPackageService(LearningRouteService routes, StudyPackageRepository repository, MaterialStorageService materials,
                        StudyPackageCache cache, StudyPackageBuilder builder, Clock clock) {
        this.routes = routes;
        this.repository = repository;
        this.materials = materials;
        this.cache = cache;
        this.builder = builder;
        this.clock = clock;
    }

    public record StudyPackage(Path path, String fileName, String contentKey, String sha256, long sizeBytes,
                               boolean fromCache) {}

    public StudyPackage packageForUnit(UUID studentId, UUID versionRouteId, String unitKey) {
        // getForStudent responde 404 si el estudiante no está inscrito o la ruta no está publicada.
        RouteDetailResponse route = routes.getForStudent(studentId, versionRouteId);
        RouteUnit unit = findUnit(route, unitKey);

        List<UUID> nodeIds = unit.nodes().stream().map(RouteNode::id).toList();
        List<Stop> stops = repository.findStops(versionRouteId, nodeIds);
        if (stops.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta unidad todavía no tiene lecciones para descargar.");
        }

        Unit content = new Unit(route.title(), route.grade().name(), route.area().name(), unit.code(), unit.title(),
                unit.competencies().stream().map(Competency::name).toList(), stops);
        List<FileRef> files = repository.findFiles(referencedFileIds(stops));
        String key = contentKey(content, files);

        try {
            var entry = cache.getOrCreate(key, out -> builder.write(content, files, this::pathOf, clock.instant(), out));
            byte[] bytes = Files.readAllBytes(entry.path());
            return new StudyPackage(entry.path(), fileName(route.title(), unit), key,
                    StudyPackageBuilder.sha256(bytes), bytes.length, entry.fromCache());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar el paquete de estudio", e);
        }
    }

    static RouteUnit findUnit(RouteDetailResponse route, String unitKey) {
        List<RouteUnit> units = route.curriculum() == null ? List.of() : route.curriculum().units();
        Optional<RouteUnit> found;
        if (LOOSE_UNIT_KEY.equalsIgnoreCase(unitKey)) {
            found = units.stream().filter(unit -> unit.id() == null).findFirst();
        } else {
            UUID unitId;
            try {
                unitId = UUID.fromString(unitKey);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La unidad solicitada no existe en esta ruta.");
            }
            found = units.stream().filter(unit -> unitId.equals(unit.id())).findFirst();
        }
        return found.filter(unit -> !unit.nodes().isEmpty()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La unidad solicitada no existe en esta ruta."));
    }

    static Set<String> referencedFileIds(List<Stop> stops) {
        Set<String> ids = new LinkedHashSet<>();
        for (Stop stop : stops) {
            stop.blocks().stream().map(Block::fileId).filter(id -> id != null).forEach(ids::add);
            stop.resources().stream().map(Resource::fileId).filter(id -> id != null).forEach(ids::add);
        }
        return ids;
    }

    static String contentKey(Unit content, List<FileRef> files) {
        record KeySource(int version, Unit unit, List<FileRef> files) {}
        byte[] json = StudyPackageBuilder.JSON.writeValueAsBytes(new KeySource(StudyPackageBuilder.FORMAT_VERSION, content, files));
        return StudyPackageBuilder.sha256(json);
    }

    static String fileName(String routeTitle, RouteUnit unit) {
        String unitPart = unit.code() != null && !"OTRAS".equals(unit.code()) ? unit.code() : unit.title();
        return StudyPackageBuilder.safeFileName("rupi-" + routeTitle + "-" + unitPart).toLowerCase(Locale.ROOT) + ".zip";
    }

    private Optional<Path> pathOf(FileRef file) {
        if (!file.id().matches("[a-fA-F0-9-]{36}")) return Optional.empty();
        return Optional.of(materials.diskPathFor(file.id(), file.originalName()));
    }
}
