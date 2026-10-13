package pe.rupi.api.studypackage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class StudyPackageCacheTest {
    private static final String KEY = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-10-13T15:00:00Z");

    @TempDir
    Path dir;

    private StudyPackageCache cache(Instant now) {
        return new StudyPackageCache(dir, Duration.ofHours(24), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("La segunda descarga sale de la caché sin volver a armar el paquete")
    void secondRequestIsHit() throws Exception {
        var builds = new AtomicInteger();
        var cache = cache(NOW);
        StudyPackageCache.Writer writer = out -> {
            builds.incrementAndGet();
            out.write("zip".getBytes(StandardCharsets.UTF_8));
        };

        var first = cache.getOrCreate(KEY, writer);
        var second = cache.getOrCreate(KEY, writer);

        assertFalse(first.fromCache());
        assertTrue(second.fromCache());
        assertEquals(1, builds.get());
        assertEquals("zip", Files.readString(second.path()));
    }

    @Test
    @DisplayName("Un paquete vencido se vuelve a armar y la limpieza borra los viejos")
    void expiredEntriesAreRebuiltAndPurged() throws Exception {
        var cache = cache(NOW);
        Path old = dir.resolve("b".repeat(64) + ".zip");
        Files.writeString(old, "viejo");
        Files.setLastModifiedTime(old, FileTime.from(NOW.minus(Duration.ofDays(3))));
        Path target = dir.resolve(KEY + ".zip");
        Files.writeString(target, "vencido");
        Files.setLastModifiedTime(target, FileTime.from(NOW.minus(Duration.ofDays(2))));

        var entry = cache.getOrCreate(KEY, out -> out.write("nuevo".getBytes(StandardCharsets.UTF_8)));

        assertFalse(entry.fromCache());
        assertEquals("nuevo", Files.readString(target));
        assertFalse(Files.exists(old));
    }

    @Test
    @DisplayName("Si falla el armado no queda un zip a medias")
    void failedBuildLeavesNothing() throws Exception {
        var cache = cache(NOW);
        assertThrows(java.io.IOException.class, () -> cache.getOrCreate(KEY, out -> {
            out.write(1);
            throw new java.io.IOException("disco lleno");
        }));
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    @DisplayName("Solo acepta claves SHA-256 en hexadecimal")
    void rejectsUnsafeKeys() {
        var cache = cache(NOW);
        assertThrows(IllegalArgumentException.class, () -> cache.getOrCreate("../../etc/passwd", out -> {}));
    }

    @Test
    @DisplayName("Varias descargas a la vez arman el paquete una sola vez")
    void concurrentRequestsBuildOnce() throws Exception {
        var builds = new AtomicInteger();
        var cache = cache(NOW);
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<StudyPackageCache.Entry>> results = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return cache.getOrCreate(KEY, out -> {
                    builds.incrementAndGet();
                    out.write("zip".getBytes(StandardCharsets.UTF_8));
                });
            }));
        }
        start.countDown();
        for (var result : results) {
            assertEquals("zip", Files.readString(result.get(10, TimeUnit.SECONDS).path()));
        }
        pool.shutdown();
        assertEquals(1, builds.get());
    }
}
