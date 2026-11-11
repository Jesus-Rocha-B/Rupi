package pe.rupi.api.calendar;
import java.util.List;
import java.time.OffsetDateTime;
public final class CalendarDtos {
    private CalendarDtos() {}
    public record Classroom(String id, String name) {}
    public record Target(String id, String title, String type) {}
    public record Event(String id, String classroomId, String classroomName, String type, String targetId,
                        String title, String start, String end, String periodName) {
        public Event(String id, String classroomId, String classroomName, String type, String targetId,
                     String title, String start, String end) {
            this(id, classroomId, classroomName, type, targetId, title, start, end, null);
        }
    }
    public record Schedule(List<Classroom> classrooms, List<Target> targets, List<Event> events) {}
    public record Dates(String classroomId, String type, String targetId, OffsetDateTime start, OffsetDateTime end) {}
}
