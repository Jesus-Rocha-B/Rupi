package pe.rupi.api.studypackage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudyPackageControllerTest {
    @TempDir Path dir;

    @Test
    void nodeEndpointRequiresSessionAndReturnsDownloadHeaders() throws Exception {
        var service = mock(StudyPackageService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new StudyPackageController(service)).build();
        var student = UUID.randomUUID();
        var route = UUID.randomUUID();
        var node = UUID.randomUUID();
        var url = "/api/v1/student/learning-routes/" + route + "/nodes/" + node + "/package";
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
        byte[] bytes = {80, 75, 3, 4};
        var file = Files.write(dir.resolve("parada.zip"), bytes);
        when(service.packageForNode(student, route, node)).thenReturn(new StudyPackageService.StudyPackage(
                file, "parada.zip", "version", StudyPackageBuilder.sha256(bytes), bytes.length, false));
        mvc.perform(get(url).principal(() -> student.toString()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"parada.zip\""))
                .andExpect(header().string("X-Rupi-Paquete-Sha256", StudyPackageBuilder.sha256(bytes)))
                .andExpect(content().bytes(bytes));
        mvc.perform(get(url).principal(() -> "invalid")).andExpect(status().isUnauthorized());
    }
}
