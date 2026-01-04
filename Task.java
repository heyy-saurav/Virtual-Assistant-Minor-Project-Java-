import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Task model representing a scheduled task with optional start/end times.
 * Contains lightweight validation to avoid invalid states.
 */
public class Task {
    public final UUID id;
    public String title;
    public int durationMinutes;
    public LocalTime startTime;
    public LocalTime endTime;
    public boolean done;

    /**
     * Create a task with explicit start and end times. Title must be non-empty.
     */
    public Task(String title, LocalTime startTime, LocalTime endTime) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Task title cannot be empty");
        if (startTime != null && endTime != null && !endTime.isAfter(startTime))
            throw new IllegalArgumentException("End time must be after start time");

        this.id = UUID.randomUUID();
        this.title = title.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = computeDurationMinutes();
        this.done = false;
    }

    /**
     * Legacy constructor: create a task by duration only.
     */
    public Task(String title, int durationMinutes) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Task title cannot be empty");
        this.id = UUID.randomUUID();
        this.title = title.trim();
        this.durationMinutes = Math.max(0, durationMinutes);
        this.startTime = null;
        this.endTime = null;
        this.done = false;
    }

    public int computeDurationMinutes() {
        if (startTime != null && endTime != null) {
            return (int) Duration.between(startTime, endTime).toMinutes();
        }
        return durationMinutes;
    }

    public UUID getId() { return id; }

    public String getTitle() { return title; }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Task title cannot be empty");
        this.title = title.trim();
    }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime))
            throw new IllegalArgumentException("End time must be after start time");
        this.startTime = startTime;
        this.durationMinutes = computeDurationMinutes();
    }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime))
            throw new IllegalArgumentException("End time must be after start time");
        this.endTime = endTime;
        this.durationMinutes = computeDurationMinutes();
    }

    public int getDurationMinutes() { return durationMinutes; }

    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = Math.max(0, durationMinutes); }

    public boolean isDone() { return done; }

    public void setDone(boolean done) { this.done = done; }

    @Override
    public String toString() {
        String times = (startTime == null ? "" : startTime.toString()) + (endTime == null ? "" : "-" + endTime.toString());
        return title + (times.isEmpty() ? "" : " (" + times + ")") + " (" + getDurationMinutes() + " min) " + (done ? "[DONE]" : "[PENDING]");
    }
}
