package pe.rupi.api.reminder;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.learningroute.LearningRouteService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderServiceTest {
    final UUID user=UUID.randomUUID(),route=UUID.randomUUID(),id=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-14T14:00:00Z");
    ReminderRepository repo; LearningRouteService routes; ReminderService service;
    @BeforeEach void setup() {
        repo=mock(ReminderRepository.class);routes=mock(LearningRouteService.class);
        service=new ReminderService(repo,routes,Clock.fixed(now,ZoneOffset.UTC));
    }
    @Test void listRequiresEnrolledRoute() {
        when(routes.getForStudent(user,route)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class,()->service.list(user,route));verifyNoInteractions(repo);
    }
    @Test void listIsScopedToAuthenticatedStudentAndRoute() {
        service.list(user,route);verify(routes).getForStudent(user,route);
        verify(repo).visible(user.toString(),route.toString(),now);
    }
    @Test void readingDoesNotGenerateReminders() {service.list(user,route);verify(repo,never()).generate(any(),any());}
    @Test void cannotMarkForeignOrObsoleteReminderRead() {
        when(repo.visible(anyString(),anyString(),any())).thenReturn(List.of());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.read(user,route,id)).getStatusCode().value());
        verify(repo,never()).markRead(any(),any(),any());
    }
    @Test void canMarkAnAlreadyReadReminderAgain() {
        when(repo.visible(anyString(),anyString(),any())).thenReturn(List.of(new ReminderRepository.Reminder(id.toString(),"room","2A","Práctica",now.plusSeconds(3600).toString(),true)));
        service.read(user,route,id);verify(repo).markRead(user.toString(),id.toString(),now);
    }
    @Test void scheduledWindowIsExactly24HoursFromUtcNow() {
        new ReminderScheduler(repo,Clock.fixed(now,ZoneOffset.UTC)).sendDueReminders();
        verify(repo).generate(now,now.plus(Duration.ofHours(24)));
    }
    @Test void schedulerRetriesAfterDatabaseFailureWithoutStopping() {
        doThrow(new DataAccessResourceFailureException("test: sin conexión")).doNothing().when(repo).generate(any(),any());
        var scheduler=new ReminderScheduler(repo,Clock.fixed(now,ZoneOffset.UTC));
        assertDoesNotThrow(scheduler::sendDueReminders);assertDoesNotThrow(scheduler::sendDueReminders);
        verify(repo,times(2)).generate(now,now.plus(Duration.ofHours(24)));
    }
}
