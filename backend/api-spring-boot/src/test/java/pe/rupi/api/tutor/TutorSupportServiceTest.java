package pe.rupi.api.tutor;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TutorSupportServiceTest {
    private static final String TEACHER="11111111-1111-4111-8111-111111111111";
    private static final String ROOM="22222222-2222-4222-8222-222222222222";
    private static final String OTHER_ROOM="33333333-3333-4333-8333-333333333333";
    private static final String ROUTE="44444444-4444-4444-8444-444444444444";
    private TutorSupportRepository repo;
    private TutorSupportService service;
    private final TutorSupportRepository.Classroom room=new TutorSupportRepository.Classroom(ROOM,"3.° A",3);

    @BeforeEach
    void setUp() { repo=Mockito.mock(TutorSupportRepository.class);service=new TutorSupportService(repo); }

    @Test
    void aulaSinConfigurarUsaPistasGuiadas() {
        when(repo.teacherClassroom(TEACHER,ROOM)).thenReturn(Optional.of(room));
        when(repo.setting(ROOM)).thenReturn(Optional.empty());
        var result=service.classroom(TEACHER,ROOM);
        assertEquals(TutorSupportService.GUIDED,result.mode());
        assertFalse(result.configured());
        assertNull(result.updatedAt());
    }

    @Test
    void guardaElModoNormalizado() {
        when(repo.teacherClassroom(TEACHER,ROOM)).thenReturn(Optional.of(room));
        when(repo.setting(ROOM)).thenReturn(Optional.of(new TutorSupportRepository.Setting("EXPLICACION_DIRECTA",Instant.now(),"Ana Quispe")));
        var result=service.save(TEACHER,ROOM," explicacion_directa ");
        verify(repo).save(ROOM,"EXPLICACION_DIRECTA",TEACHER);
        assertEquals("EXPLICACION_DIRECTA",result.mode());
        assertTrue(result.configured());
    }

    @Test
    void rechazaUnModoDesconocidoSinGuardar() {
        var ex=assertThrows(ResponseStatusException.class,()->service.save(TEACHER,ROOM,"RESPUESTA_FINAL"));
        assertEquals(HttpStatus.BAD_REQUEST,ex.getStatusCode());
        verify(repo,never()).save(any(),any(),any());
    }

    @Test
    void noPermiteConfigurarUnAulaAjena() {
        when(repo.teacherClassroom(TEACHER,ROOM)).thenReturn(Optional.empty());
        var ex=assertThrows(ResponseStatusException.class,()->service.save(TEACHER,ROOM,"PISTAS_GUIADAS"));
        assertEquals(HttpStatus.NOT_FOUND,ex.getStatusCode());
        verify(repo,never()).save(any(),any(),any());
    }

    @Test
    void estudianteSinAulaRecibeElModoPredeterminado() {
        when(repo.routeClassrooms(TEACHER,ROUTE)).thenReturn(List.of());
        var result=service.student(TEACHER,ROUTE);
        assertEquals(TutorSupportService.GUIDED,result.mode());
        assertEquals("PREDETERMINADO",result.source());
    }

    @Test
    void conVariasAulasPrefiereLasPistasGuiadas() {
        var other=new TutorSupportRepository.Classroom(OTHER_ROOM,"3.° B",3);
        when(repo.routeClassrooms(TEACHER,ROUTE)).thenReturn(List.of(room,other));
        when(repo.setting(ROOM)).thenReturn(Optional.of(new TutorSupportRepository.Setting("EXPLICACION_DIRECTA",Instant.now(),"Ana")));
        when(repo.setting(OTHER_ROOM)).thenReturn(Optional.empty());
        var result=service.student(TEACHER,ROUTE);
        assertEquals(TutorSupportService.GUIDED,result.mode());
        assertEquals("3.° B",result.classroomName());
    }

    @Test
    void estudianteRecibeLaExplicacionDirectaDeSuAula() {
        when(repo.routeClassrooms(TEACHER,ROUTE)).thenReturn(List.of(room));
        when(repo.setting(ROOM)).thenReturn(Optional.of(new TutorSupportRepository.Setting("EXPLICACION_DIRECTA",Instant.now(),"Ana")));
        var result=service.student(TEACHER,ROUTE);
        assertEquals("EXPLICACION_DIRECTA",result.mode());
        assertEquals("AULA",result.source());
    }
}
