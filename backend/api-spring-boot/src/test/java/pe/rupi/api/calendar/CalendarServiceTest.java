package pe.rupi.api.calendar;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.rupi.api.learningroute.LearningRouteService;
import static pe.rupi.api.calendar.CalendarDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CalendarServiceTest {
    CalendarRepository repo; LearningRouteService routes; CalendarService service;
    String user="11111111-1111-4111-8111-111111111111", route="22222222-2222-4222-8222-222222222222";
    Dates dates(String room,String type,String target,String start,String end) {return new Dates(room,type,target,OffsetDateTime.parse(start),OffsetDateTime.parse(end));}
    Dates valid() {return dates("room","UNIDAD","unit","2026-10-15T08:00:00-05:00","2026-10-15T10:00:00-05:00");}
    @BeforeEach void setup() {
        repo=mock(CalendarRepository.class);routes=mock(LearningRouteService.class);service=new CalendarService(repo,routes);
        when(repo.owns(user,route)).thenReturn(true);
        when(repo.classrooms(user,route,true)).thenReturn(List.of(new Classroom("room","2 A")));
        when(repo.targets(route)).thenReturn(List.of(new Target("unit","Unidad 1","UNIDAD")));
    }
    @Test void savesOnlyValidatedRouteTarget() {service.save(user,route,valid());verify(repo).save(user,route,valid(),new Target("unit","Unidad 1","UNIDAD"));}
    @Test void rejectsEndBeforeStart() {assertThrows(ResponseStatusException.class,()->CalendarService.validate(dates("room","UNIDAD","unit","2026-10-15T10:00:00-05:00","2026-10-15T09:00:00-05:00")));}
    @Test void comparesInstantsAcrossOffsetsAndAllowsSameInstant() {CalendarService.validate(dates("room","UNIDAD","unit","2026-10-15T08:00:00-05:00","2026-10-15T13:00:00Z"));}
    @Test void rejectsMissingAndUnsupportedType() {assertThrows(ResponseStatusException.class,()->CalendarService.validate(null));assertThrows(ResponseStatusException.class,()->CalendarService.validate(dates("room","OTRO","unit","2026-10-15T08:00:00Z","2026-10-15T09:00:00Z")));}
    @Test void rejectsForeignRouteBeforeWriting() {when(repo.owns(user,route)).thenReturn(false);assertEquals(404,assertThrows(ResponseStatusException.class,()->service.save(user,route,valid())).getStatusCode().value());verify(repo,never()).save(any(),any(),any(),any());}
    @Test void rejectsForeignClassroom() {when(repo.classrooms(user,route,true)).thenReturn(List.of());assertEquals(403,assertThrows(ResponseStatusException.class,()->service.save(user,route,valid())).getStatusCode().value());verify(repo,never()).save(any(),any(),any(),any());}
    @Test void rejectsTargetOutsideRoute() {when(repo.targets(route)).thenReturn(List.of());assertEquals(400,assertThrows(ResponseStatusException.class,()->service.save(user,route,valid())).getStatusCode().value());}
    @Test void studentMustBeEnrolledAndOnlyReceivesOwnClassrooms() {var rooms=List.of(new Classroom("student-room","2 B"));when(repo.classrooms(user,route,false)).thenReturn(rooms);var result=service.read(user,route,false);verify(routes).getForStudent(java.util.UUID.fromString(user),java.util.UUID.fromString(route));verify(repo).events(route,rooms);assertTrue(result.targets().isEmpty());}
    @Test void deleteCannotCrossClassrooms() {assertThrows(ResponseStatusException.class,()->service.delete(user,route,"other-room","event"));verify(repo,never()).delete(any(),any(),any());}
    @Test void deleteMissingEventIs404() {assertEquals(404,assertThrows(ResponseStatusException.class,()->service.delete(user,route,"room","missing")).getStatusCode().value());}
}
