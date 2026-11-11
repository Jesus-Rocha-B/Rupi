package pe.rupi.api.calendar;

import java.sql.Date;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalendarPeriodTest {
    @Test void usesPeruvianDatesAtUtcBoundary() {
        assertEquals("I bimestre", CalendarRepository.periodName("I bimestre", Date.valueOf("2027-03-01"), Date.valueOf("2027-05-14"),
            Timestamp.valueOf("2027-05-15 02:00:00"), Timestamp.valueOf("2027-05-15 04:59:00")));
    }
    @Test void hidesStaleAssignmentWhenTeacherMovesDates() {
        assertNull(CalendarRepository.periodName("I bimestre", Date.valueOf("2027-03-01"), Date.valueOf("2027-05-14"),
            Timestamp.valueOf("2027-05-15 05:00:00"), Timestamp.valueOf("2027-05-15 06:00:00")));
        assertNull(CalendarRepository.periodName(null, null, null, null, null));
    }
}
