package pe.rupi.api.reminder;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@EnableScheduling
@ConditionalOnProperty(name="rupi.reminders.enabled", havingValue="true", matchIfMissing=true)
public class ReminderScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(ReminderScheduler.class);
    static final Duration NOTICE = Duration.ofHours(24);
    private final ReminderRepository repository;
    private final Clock clock;
    @Autowired
    public ReminderScheduler(ReminderRepository repository) { this(repository,Clock.systemUTC()); }
    ReminderScheduler(ReminderRepository repository, Clock clock) { this.repository=repository;this.clock=clock; }

    @Scheduled(fixedDelayString="${rupi.reminders.poll-ms:60000}", initialDelayString="${rupi.reminders.initial-delay-ms:10000}")
    public void sendDueReminders() {
        var now=clock.instant();
        try { repository.generate(now,now.plus(NOTICE)); }
        catch (DataAccessException error) {
            // La siguiente ejecución vuelve a intentarlo. La restricción única conserva los envíos anteriores.
            LOG.error("No se pudieron preparar los recordatorios HU-14; se reintentará en el próximo ciclo. Comprueba V14 y los permisos de la API.", error);
        }
    }
}
