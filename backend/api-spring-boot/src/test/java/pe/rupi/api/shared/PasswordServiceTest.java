package pe.rupi.api.shared;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {

    private final PasswordService service = new PasswordService();

    @Test
    void bcryptMatchesCorrectPassword() {
        String hash = service.encode("123456");
        assertTrue(service.matches("123456", hash));
        assertFalse(service.matches("wrongpass", hash));
    }

    @Test
    void matchesHandlesNullAndBlankSafely() {
        assertFalse(service.matches(null, "somehash"));
        assertFalse(service.matches("pass", null));
        assertFalse(service.matches("pass", "   "));
        assertFalse(service.matches("pass", "unsupported$hash"));
    }

    @Test
    void performDummyCheckExecutesSafely() {
        assertDoesNotThrow(() -> service.performDummyCheck("test-password"));
        assertDoesNotThrow(() -> service.performDummyCheck(null));
    }
}
