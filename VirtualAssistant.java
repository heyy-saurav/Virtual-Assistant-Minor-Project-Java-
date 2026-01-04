import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class VirtualAssistant {
    public final Scanner in = new Scanner(System.in);
    public final Map<LocalDate, List<Task>> schedules = new HashMap<>();
    public String userName;

    public static void main(String[] args) {
        new VirtualAssistant().start();
    }

    public void start() {
        greetAndGetName();
        startReminderThread();
        mainLoop();
    }

    // Reminder thread: notifies 5 minutes before a task start and reminds for breaks
    public void startReminderThread() {
        // Use a background daemon thread so it doesn't block program exit
        Thread reminder = new Thread(() -> {
            // Track which task start reminders we've already shown (by id)
            final Set<java.util.UUID> remindedStarts = Collections.synchronizedSet(new HashSet<>());
            // Track when focused work started (epoch seconds) to detect 1 hour continuous work
            long focusedStart = -1L;
            while (true) {
                try {
                    LocalDate today = LocalDate.now();
                    LocalDateTime now = LocalDateTime.now();
                    List<Task> todays = schedules.getOrDefault(today, Collections.emptyList());
                    // Sort tasks by start time if available
                    List<Task> sorted = todays.stream()
                            .filter(t -> t.getStartTime() != null)
                            .sorted(Comparator.comparing(Task::getStartTime))
                            .collect(Collectors.toList());

                    boolean anyTaskInProgress = false;
                    for (Task t : sorted) {
                        if (t.isDone()) continue;
                        LocalTime st = t.getStartTime();
                        LocalTime et = t.getEndTime();
                        if (st == null) continue;
                        LocalDateTime taskStart = LocalDateTime.of(today, st);
                        LocalDateTime taskEnd = et == null ? taskStart.plusMinutes(t.getDurationMinutes()) : LocalDateTime.of(today, et);

                        // Reminder 5 minutes before start
                        if (!remindedStarts.contains(t.getId())) {
                            LocalDateTime remindAt = taskStart.minusMinutes(5);
                            if (!now.isBefore(remindAt) && now.isBefore(taskStart)) {
                                System.out.println("\n[Reminder] Upcoming task in 5 minutes: " + t.getTitle() + " (starts at " + st + ")");
                                remindedStarts.add(t.getId());
                            }
                        }

                        // If task currently in progress
                        if (!now.isBefore(taskStart) && now.isBefore(taskEnd)) {
                            anyTaskInProgress = true;
                        }
                    }

                    // Break reminder: if continuous focused work reaches 60 minutes, remind and reset
                    if (anyTaskInProgress) {
                        if (focusedStart == -1L) focusedStart = Instant.now().getEpochSecond();
                        long elapsedSeconds = Instant.now().getEpochSecond() - focusedStart;
                        if (elapsedSeconds >= 60 * 60) {
                            System.out.println("[Break Reminder] You've been focused for 1 hour. Take a 5-10 minute break.");
                            // reset focusedStart to now so reminder repeats after another hour
                            focusedStart = Instant.now().getEpochSecond();
                        }
                    } else {
                        // no active task — reset focused timer
                        focusedStart = -1L;
                    }

                    Thread.sleep(30 * 1000); // check every 30 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    // Keep the thread alive on other exceptions
                    System.out.println("[Reminder Thread Error] " + ex.getMessage());
                    try { Thread.sleep(30 * 1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }, "VA-Reminder-Thread");
        reminder.setDaemon(true);
        reminder.start();
    }

    public void greetAndGetName() {
        System.out.print("Hello! What's your name? \n");
        userName = in.nextLine().trim();
        if (userName.isEmpty()) userName = "User";
        System.out.println("Nice to meet you, " + userName + "!");
        System.out.println("How may I help you ?");
    }

    public void mainLoop() {
        while (true) {
            printMainMenu();
            int choice = readInt("Choose an option: \n");
            switch (choice) {
                case 1 -> showTimeDayDate();
                case 2 -> createScheduleForDay();
                case 4 -> editScheduleMenu();
                case 3 -> showScheduleMenu();
                case 5 -> accomplishTasksMenu();
                case 6 -> {
                    System.out.println("Goodbye, " + userName + ". Have a productive day!");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
            System.out.println();
        }
    }

    public void printMainMenu() {
        System.out.println("\n----- Main Menu -----");
        System.out.println("\n1) What is the Time ?");
        System.out.println("2) Make schedule for the day");
        System.out.println("3) Show schedule");
        System.out.println("4) Edit schedule");
        System.out.println("5) Mark accomplished tasks & show progress");
        System.out.println("6) Exit\n");
    }

    // Option 1
    public void showTimeDayDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm:ss a");
        System.out.println("\nCurrent date: " + now.format(dtf));
        System.out.println("\nCurrent time: " + now.format(tf));
        System.out.println("\nDay of week: " + now.getDayOfWeek() + "\n");
    }

    // Option 2
    public void createScheduleForDay() {
        LocalDate date = readDate("Enter date for schedule (yyyy-MM-dd): ");
        List<Task> tasks = schedules.computeIfAbsent(date, d -> new ArrayList<>());
        System.out.println("Creating / editing schedule for " + date);
        while (true) {
            System.out.println("1) Add task");
            System.out.println("2) Finish");
            int c = readInt("Choose: ");
            if (c == 1) {
                addTaskToList(tasks);
            } else if (c == 2) {
                break;
            } else {
                System.out.println("Invalid option");
            }
        }
        schedules.put(date, tasks);
        System.out.println("Saved schedule for " + date + " (" + tasks.size() + " tasks).");
    }

    public void addTaskToList(List<Task> tasks) {
        System.out.print("\nTask title: ");
        String title = in.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("\nTask cannot be empty.\n");
            return;
        }
        System.out.println("\nEnter start time in 24-hour format HH:mm (e.g., 09:30): ");
        LocalTime start = readTime("Start time (HH:mm): ");
        System.out.println("\nEnter end time in 24-hour format HH:mm (e.g., 10:15). Leave blank if unknown:");
        LocalTime end = readTimeAllowBlank("End time (HH:mm) or blank: ");
        
        // Validate end time is after start time
        if (end != null && !end.isAfter(start)) {
            System.out.println("\nEnd time must be after start time. Task not added.\n");
            return;
        }

        // Check for overlaps
        if (hasOverlap(tasks, start, end, null)) {
            System.out.println("\nWarning: This time slot overlaps with an existing task!");
            int duration = end != null ? (int)Duration.between(start, end).toMinutes() : 60; // default 1 hour
            LocalTime nextSlot = findNextAvailableSlot(tasks, duration);
            
            if (nextSlot != null) {
                System.out.println("\nNext available slot: " + nextSlot + " - " + nextSlot.plusMinutes(duration));
                System.out.print("\nWould you like to use this slot instead? (y/n): ");
                String answer = in.nextLine().trim().toLowerCase();
                if (answer.startsWith("y")) {
                    start = nextSlot;
                    end = nextSlot.plusMinutes(duration);
                } else {
                    System.out.print("\nWould you like to add the task anyway? (y/n): ");
                    answer = in.nextLine().trim().toLowerCase();
                    if (!answer.startsWith("y")) {
                        System.out.println("\nTask not added.\n");
                        return;
                    }
                }
            } else {
                System.out.println("\nNo available slots found today. Consider scheduling for another day.");
                System.out.print("\nWould you like to add the task anyway? (y/n): ");
                String answer = in.nextLine().trim().toLowerCase();
                if (!answer.startsWith("y")) {
                    System.out.println("\nTask not added.\n");
                    return;
                }
            }
        }
        
        Task t = new Task(title, start, end);
        tasks.add(t);
        System.out.println("\nAdded: " + t + "\n");
    }

    // Option 3
    public void editScheduleMenu() {
        LocalDate date = readDate("Enter date of schedule to edit (yyyy-MM-dd): ");
        List<Task> tasks = schedules.get(date);
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("No schedule found for " + date + ". You can create one instead.");
            return;
        }
        while (true) {
            System.out.println("Editing schedule for " + date);
            printTasksBrief(tasks);
            System.out.println("\n\n1) Add task");
            System.out.println("2) Delete task");
            System.out.println("3) Modify task");
            System.out.println("4) Back to main menu");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> addTaskToList(tasks);
                case 2 -> deleteTask(tasks);
                case 3 -> modifyTask(tasks);
                case 4 -> {
                    schedules.put(date, tasks);
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

    public void deleteTask(List<Task> tasks) {
        int idx = readInt("Enter task number to delete: ") - 1;
        if (idx < 0 || idx >= tasks.size()) {
            System.out.println("Invalid task number.");
            return;
        }
        Task removed = tasks.remove(idx);
        System.out.println("Removed: " + removed);
    }

    public void modifyTask(List<Task> tasks) {
        int idx = readInt("Enter task number to modify: ") - 1;
        if (idx < 0 || idx >= tasks.size()) {
            System.out.println("Invalid task number.");
            return;
        }
        Task t = tasks.get(idx);
        System.out.println("Current: " + t);
        System.out.print("New title (leave blank to keep): ");
        String title = in.nextLine().trim();
        if (!title.isEmpty()) t.setTitle(title);
        System.out.print("New start time (HH:mm) (leave blank to keep): ");
        String newStart = in.nextLine().trim();
        LocalTime newStartTime = null;
        if (!newStart.isEmpty()) {
            try {
                newStartTime = LocalTime.parse(newStart);
            } catch (Exception e) {
                System.out.println("Invalid time format. Start time unchanged.");
            }
        }

        System.out.print("New end time (HH:mm) (leave blank to keep or enter blank to unset): ");
        String newEnd = in.nextLine().trim();
        LocalTime newEndTime = null;
        if (!newEnd.isEmpty()) {
            try {
                newEndTime = LocalTime.parse(newEnd);
            } catch (Exception e) {
                System.out.println("Invalid time format. End time unchanged.");
            }
        }

        // Use existing times if new ones weren't provided
        LocalTime finalStart = newStartTime != null ? newStartTime : t.getStartTime();
        LocalTime finalEnd = newEndTime != null ? newEndTime : t.getEndTime();

        // Validate times if both are provided
        if (finalStart != null && finalEnd != null && !finalEnd.isAfter(finalStart)) {
            System.out.println("End time must be after start time. Times not updated.");
            return;
        }

        // Check for overlaps with other tasks
        if (hasOverlap(tasks, finalStart, finalEnd, t)) {
            System.out.println("Warning: These times would overlap with another task!");
            System.out.print("Would you like to update anyway? (y/n): ");
            String answer = in.nextLine().trim().toLowerCase();
            if (!answer.startsWith("y")) {
                System.out.println("Times not updated.");
                return;
            }
        }

        // Update times if we got here
        if (newStartTime != null) t.setStartTime(newStartTime);
        if (newEndTime != null) t.setEndTime(newEndTime);
        System.out.println("Modified: " + t);
    }

    // Option 4
    public void showScheduleMenu() {
        LocalDate date = readDate("Enter date to view schedule (yyyy-MM-dd): ");
        List<Task> tasks = schedules.getOrDefault(date, Collections.emptyList());
        if (tasks.isEmpty()) {
            System.out.println("No tasks scheduled for " + date);
            return;
        }
        while (true) {
            System.out.println("Viewing schedule for " + date);
            System.out.println("1) Show current task (first uncompleted)");
            System.out.println("2) Show full day schedule");
            System.out.println("3) Back");
            int c = readInt("Choose: ");
            if (c == 1) {
                Optional<Task> current = tasks.stream().filter(t -> !t.isDone()).findFirst();
                if (current.isPresent()) {
                    System.out.println("Current task: " + current.get());
                    return;
                } else {
                    System.out.println("No pending tasks. All done!");
                    return;
                }
            } else if (c == 2) {
                printTasksDetailed(tasks);
                return;
            } else if (c == 3) {
                return;
            } else {
                System.out.println("Invalid option");
                return;
            }
        }
    }

    // Option 5
    public void accomplishTasksMenu() {
        LocalDate date = readDate("Enter date of schedule to mark accomplished (yyyy-MM-dd): ");
        List<Task> tasks = schedules.get(date);
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("No schedule for " + date);
            return;
        }
        while (true) {
            System.out.println("Mark accomplished tasks for " + date);
            System.out.println("1) Mark a task done now");
            System.out.println("2) Mark multiple tasks done (provide numbers separated by commas)");
            System.out.println("3) Mark all at once (EOD)");
            System.out.println("4) Show progress");
            System.out.println("5) Back");
            int c = readInt("Choose: ");
            switch (c) {
                case 1 -> markSingleTaskDone(tasks);
                case 2 -> markMultipleTasksDone(tasks);
                case 3 -> {
                    tasks.forEach(t -> t.setDone(true));
                    System.out.println("All tasks marked done.");
                }
                case 4 -> showProgress(tasks);
                case 5 -> {
                    schedules.put(date, tasks);
                    return;
                }
                default -> System.out.println("Invalid option");
            }
        }
    }

    public void markSingleTaskDone(List<Task> tasks) {
        printTasksBrief(tasks);
        int idx = readInt("Enter task number completed: ") - 1;
        if (idx < 0 || idx >= tasks.size()) {
            System.out.println("Invalid number.");
            return;
        }
        Task t = tasks.get(idx);
        t.setDone(true);
        System.out.println("Marked done: " + t.getTitle());
    }

    public void markMultipleTasksDone(List<Task> tasks) {
        System.out.print("Enter task numbers separated by commas (e.g., 1,3,4): ");
        String line = in.nextLine().trim();
        if (line.isEmpty()) {
            System.out.println("No input given.");
            return;
        }
        String[] parts = line.split(",");
        int marked = 0;
        for (String p : parts) {
            try {
                int idx = Integer.parseInt(p.trim()) - 1;
                if (idx >= 0 && idx < tasks.size()) {
                    tasks.get(idx).setDone(true);
                    marked++;
                }
            } catch (NumberFormatException ignored) {}
        }
        System.out.println("Marked " + marked + " tasks done.");
    }

    public void showProgress(List<Task> tasks) {
        long total = tasks.size();
        long done = tasks.stream().filter(Task::isDone).count();
        double percent = total == 0 ? 100.0 : (done * 100.0 / total);
        System.out.println("\nProgress: " + done + " / " + total + " tasks completed (" +
                String.format("%.1f", percent) + "%)\n");
        if (done < total) {
            System.out.println("Pending tasks:");
            tasks.stream().filter(t -> !t.isDone()).forEach(t -> System.out.println("\n - " + t.getTitle()));
            System.out.println();
        } else {
            System.out.println("\nAll tasks completed. Good job!\n");
        }
    }

    // Helpers
    public LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            try {
                return LocalDate.parse(s);
            } catch (Exception e) {
                System.out.println("Invalid format. Please use yyyy-MM-dd.");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            try {
                return LocalTime.parse(s);
            } catch (Exception e) {
                System.out.println("Invalid time. Please use HH:mm (24-hour).");
            }
        }
    }

    public LocalTime readTimeAllowBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            if (s.isEmpty()) return null;
            try {
                return LocalTime.parse(s);
            } catch (Exception e) {
                System.out.println("Invalid time. Please use HH:mm (24-hour) or leave blank.");
            }
        }
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    // Time slot management helpers
    public boolean hasOverlap(List<Task> tasks, LocalTime start, LocalTime end, Task excludeTask) {
        if (start == null || end == null) return false;
        return tasks.stream()
                .filter(t -> t != excludeTask && !t.isDone()) // ignore done tasks and the task being modified
                .filter(t -> t.getStartTime() != null && t.getEndTime() != null)
                .anyMatch(t -> {
                    // Check if either start or end time falls within another task's time range
                    return (!start.isAfter(t.getEndTime()) && !end.isBefore(t.getStartTime()));
                });
    }

    public LocalTime findNextAvailableSlot(List<Task> tasks, int durationMinutes) {
        if (tasks.isEmpty()) return LocalTime.of(9, 0); // Default to 9 AM if no tasks

        // Get all tasks with times, sorted by start time
        List<Task> scheduledTasks = tasks.stream()
                .filter(t -> !t.isDone() && t.getStartTime() != null && t.getEndTime() != null)
                .sorted(Comparator.comparing(Task::getStartTime))
                .collect(Collectors.toList());

        if (scheduledTasks.isEmpty()) return LocalTime.of(9, 0);

        // Start with work day beginning (9 AM)
        LocalTime slot = LocalTime.of(9, 0);
        
        // Try each potential slot
        for (Task task : scheduledTasks) {
            // If there's room before this task
            LocalTime taskStart = task.getStartTime();
            if (slot.plusMinutes(durationMinutes).isBefore(taskStart) || 
                slot.plusMinutes(durationMinutes).equals(taskStart)) {
                return slot; // We found a slot that fits
            }
            // Move to end of current task
            slot = task.getEndTime();
        }

        // If we get here, try after the last task
        if (slot.plusMinutes(durationMinutes).isBefore(LocalTime.of(17, 0))) {
            return slot; // Return slot after last task if it ends before 5 PM
        }

        // No slot found today
        return null;
    }

    public void printTasksBrief(List<Task> tasks) {
        System.out.println();  // Add space before task list
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            String times = (t.getStartTime() == null ? "" : t.getStartTime().toString()) + (t.getEndTime() == null ? "" : "-" + t.getEndTime().toString());
            System.out.printf("%d) %s %s %s%n", i + 1, t.getTitle(), times.isEmpty() ? "" : "(" + times + ")", t.isDone() ? "[DONE]" : "");
        }
        System.out.println();  // Add space after task list
    }

    public void printTasksDetailed(List<Task> tasks) {
        System.out.println("\nFull schedule:\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            String start = t.getStartTime() == null ? "--" : t.getStartTime().toString();
            String end = t.getEndTime() == null ? "--" : t.getEndTime().toString();
            System.out.printf("%d) %s | %s - %s | %d min | %s%n", i + 1, t.getTitle(), start, end, t.getDurationMinutes(),
                    t.isDone() ? "DONE" : "PENDING");
        }
        int totalDuration = tasks.stream().mapToInt(Task::getDurationMinutes).sum();
        System.out.println("\nTotal tasks: " + tasks.size() + " | Total estimated minutes: " + totalDuration + "\n");
    }

    // Task class
    public static class Task {
        public final java.util.UUID id;
        public String title;
        // optional fallback duration (minutes) if times are not provided
        public int durationMinutes;
        public LocalTime startTime;
        public LocalTime endTime;
        public boolean done;

        Task(String title, LocalTime startTime, LocalTime endTime) {
            this.id = java.util.UUID.randomUUID();
            this.title = title;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationMinutes = 0;
            this.done = false;
        }

        // Legacy constructor (kept for safety) - will set durationMinutes only
        Task(String title, int durationMinutes) {
            this.id = java.util.UUID.randomUUID();
            this.title = title;
            this.durationMinutes = Math.max(0, durationMinutes);
            this.startTime = null;
            this.endTime = null;
            this.done = false;
        }

        java.util.UUID getId() { return id; }

        String getTitle() { return title; }

        void setTitle(String title) { this.title = title; }

        LocalTime getStartTime() { return startTime; }

        void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        LocalTime getEndTime() { return endTime; }

        void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        int getDurationMinutes() {
            if (startTime != null && endTime != null) {
                long mins = java.time.Duration.between(startTime, endTime).toMinutes();
                return (int)Math.max(0, mins);
            }
            return durationMinutes;
        }

        void setDurationMinutes(int durationMinutes) { this.durationMinutes = Math.max(0, durationMinutes); }

        boolean isDone() { return done; }

        void setDone(boolean done) { this.done = done; }

        @Override
        public String toString() {
            String times = (startTime == null ? "" : startTime.toString()) + (endTime == null ? "" : "-" + endTime.toString());
            return title + (times.isEmpty() ? "" : " (" + times + ")") + " (" + getDurationMinutes() + " min) " + (done ? "[DONE]" : "[PENDING]");
        }
    }
}