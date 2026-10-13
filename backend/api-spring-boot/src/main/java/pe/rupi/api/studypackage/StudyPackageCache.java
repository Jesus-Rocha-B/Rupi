package pe.rupi.api.studypackage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda en disco los zip ya armados. La clave es el SHA-256 del contenido de la unidad, así que un cambio del
 * docente produce otra clave y el paquete anterior simplemente deja de pedirse; la limpieza por antigüedad lo borra.
 */
@Component
public class StudyPackageCache {
    @FunctionalInterface
    public interface Writer {
        void write(OutputStream out) throws IOException;
    }

    public record Entry(Path path, boolean fromCache) {}

    private final Path directory;
    private final Duration ttl;
    private final Clock clock;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    @Autowired
    public StudyPackageCache(
            @Value("${rupi.storage.package-cache-path:uploads/package-cache}") String directory,
            @Value("${rupi.storage.package-cache-ttl-hours:168}") long ttlHours
    ) {
        this(Paths.get(directory), Duration.ofHours(ttlHours), Clock.systemUTC());
    }

    StudyPackageCache(Path directory, Duration ttl, Clock clock) {
        this.directory = directory.toAbsolutePath().normalize();
        this.ttl = ttl;
        this.clock = clock;
        try {
            Files.createDirectories(this.directory);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo preparar la carpeta de paquetes: " + this.directory, e);
        }
    }

    public Entry getOrCreate(String key, Writer writer) throws IOException {
        if (key == null || !key.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Clave de paquete inválida");
        }
        Path target = directory.resolve(key + ".zip");
        if (isFresh(target)) {
            touch(target);
            return new Entry(target, true);
        }

        Object lock = locks.computeIfAbsent(key, k -> new Object());
        try {
            synchronized (lock) {
                if (isFresh(target)) {
                    touch(target);
                    return new Entry(target, true);
                }
                Path temporary = Files.createTempFile(directory, key, ".tmp");
                try {
                    try (OutputStream out = Files.newOutputStream(temporary)) {
                        writer.write(out);
                    }
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        } finally {
            locks.remove(key, lock);
        }
        purgeExpired();
        return new Entry(target, false);
    }

    /** Borra los paquetes que nadie pidió durante el tiempo de vida y los temporales abandonados. */
    public int purgeExpired() throws IOException {
        Instant limit = clock.instant().minus(ttl);
        int removed = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.{zip,tmp}")) {
            for (Path file : files) {
                try {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(limit) && Files.deleteIfExists(file)) {
                        removed++;
                    }
                } catch (IOException ignored) {
                    // Otro hilo pudo borrarlo o reemplazarlo entre la lectura y el borrado.
                }
            }
        }
        return removed;
    }

    private boolean isFresh(Path file) throws IOException {
        return Files.isRegularFile(file)
                && !Files.getLastModifiedTime(file).toInstant().isBefore(clock.instant().minus(ttl));
    }

    private void touch(Path file) throws IOException {
        Files.setLastModifiedTime(file, FileTime.from(clock.instant()));
    }
}
