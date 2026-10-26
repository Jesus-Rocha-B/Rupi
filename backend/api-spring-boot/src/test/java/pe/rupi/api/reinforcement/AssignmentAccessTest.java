package pe.rupi.api.reinforcement;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import pe.rupi.api.shared.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssignmentAccessTest {
    @Test void missingSessionCannotReachTeacherOrStudentEndpoints() throws Exception {
        var sessions=mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.empty());
        for(String role:new String[]{"teacher","student"}) {
            var response=new MockHttpServletResponse();
            new StudentAuthenticationFilter(sessions).doFilter(
                new MockHttpServletRequest("GET","/api/v1/"+role+"/reinforcement-assignments"),response,(req,res)->fail("Unauthorized access"));
            assertEquals(401,response.getStatus());
        }
    }
    @Test void studentCannotReachTeacherAssignments() throws Exception {
        var sessions=mock(StudentSessionService.class);
        when(sessions.resolve(any())).thenReturn(Optional.of(new StudentSessionService.Student("10000000-0000-4000-8000-000000000001","Ana")));
        var response=new MockHttpServletResponse();
        new StudentAuthenticationFilter(sessions).doFilter(new MockHttpServletRequest("POST","/api/v1/teacher/reinforcement-assignments"),response,(req,res)->fail("Student reached teacher mutation"));
        assertEquals(403,response.getStatus());
    }
    @Test void crossOriginChangesAreRejectedBeforeService() {
        var service=mock(AssignmentService.class);
        var request=new MockHttpServletRequest();
        request.addHeader("Origin","https://foreign.example");
        var ex=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->
            new AssignmentController(service).start(java.util.UUID.randomUUID(),()->java.util.UUID.randomUUID().toString(),request));
        assertEquals(403,ex.getStatusCode().value());verifyNoInteractions(service);
    }
}
