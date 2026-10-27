package pe.rupi.api.notification;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StreakReminderTimeTest {
    static StudyNotificationService.Preferences prefs(String remindAt, String quietFrom, String quietUntil) {
        return new StudyNotificationService.Preferences(1, remindAt, true, true, false, quietFrom, quietUntil, 0);
    }

    @Test
    void remindsTwoHoursBeforeRest() {
        assertEquals(LocalTime.of(19, 0), StudyNotificationService.streakAt(prefs("17:00", "21:00", "08:00")));
    }

    @Test
    void neverBeforeTheChosenReminderTime() {
        assertEquals(LocalTime.of(20, 30), StudyNotificationService.streakAt(prefs("20:30", "21:00", "08:00")));
    }

    @Test
    void restAfterMidnightStillWorks() {
        assertEquals(LocalTime.of(22, 30), StudyNotificationService.streakAt(prefs("17:00", "00:30", "07:00")));
    }
}
