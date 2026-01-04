# VAMP — GUI Pseudocode

Overview
- Java Swing GUI to add/view/edit daily tasks, send reminders, and show notifications.
- In-memory schedules: Map<LocalDate, List<Task>>. Task: {id, title, startTime, endTime, duration, done}.

Startup
- Main: set LookAndFeel, create `VirtualAssistantGUI` on EDT.
- Init: schedules, system tray (optional), UI (header, task list, buttons), clock timer, RGB timer, reminder daemon.
- After 300ms show name dialog (Save/Skip). Save displays greeting then replaces it after 5s.

Primary UI
- Header: gradient, greeting label (hidden initially), textual clock (updates every second).
- Center: `TaskListPanel` (today's tasks).
- Footer: Buttons — Add Task, View Schedule, Launch App.

Add Task (modal)
- Inputs: Title, Date (Y/M/D spinners), Start time (H/M spinners), Duration (minutes).
- Add: validate title, build LocalDate/LocalTime, compute endTime, create Task, add to schedules[date], refresh UI, notify, close.
- Cancel: close without saving.

View Schedule (modal)
- Left: date selector; Right: scrollable task list for selected date.
- Rebuild list on date change. Each row shows title/time + Edit/Delete.
- Delete: confirm → remove from schedules → refresh.

Edit Task (modal)
- Pre-fill fields; on Save validate title/time (HH:mm), update task fields, refresh UI, close.

Reminder Thread (daemon, every 30s)
- Track `remindedStarts` (synchronized) and `focusedStart` epoch.
- For today's tasks (sorted): skip done. Compute taskStart/taskEnd and remindAt = taskStart - 5min.
- If now in [remindAt, taskStart) and id not reminded: show 5-min notification, play sound, add id to set.
- If now in [taskStart, taskEnd): mark in-progress.
- If continuous focus >= 3600s: notify break and reset focus timer.

Notifications & Sound
- Use SystemTray.displayMessage if supported; attempt to play `notification.wav` (ignore errors).

Styling & Effects
- Dark theme via `Colors` constants. Buttons styled with rounded gradients. Header hue rotates via HSB timer (50ms).

Threading & Safety
- UI updates on EDT. Reminder runs in background (daemon). `schedules` accessed on EDT; `remindedStarts` synchronized.

Persistence
- Runtime-only currently; suggest JSON save/load on exit/startup as an improvement.

Error Handling (short)
- Validation/parsing errors: show message and keep modal open.
- SystemTray/audio errors: log/skip, do not crash UI.
- Reminder thread: handle InterruptedException and log other exceptions, continue loop.

Quick Flow Summary
1) Start → init UI and threads → show name dialog.
2) User adds/edits/deletes tasks; tasks stored in schedules map.
3) Reminder thread checks tasks every 30s: send 5-min reminders and 1-hour break prompts.
4) Notifications via system tray; sound optional.

This concise version preserves the original program flow and behaviors while removing verbose examples and repetition.
