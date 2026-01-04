import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;

public class EditTaskDialog extends JDialog {
    public static final long serialVersionUID = 1L;

    public final JFrame parent;
    public transient final Task task;
    public final java.time.LocalDate taskDate;
    public transient final Runnable onSave;

    public EditTaskDialog(JFrame parent, Task task, java.time.LocalDate taskDate, Runnable onSave) {
        super((java.awt.Frame) null, "Edit Task", true);
        this.parent = parent;
        this.task = task;
        this.taskDate = taskDate;
        this.onSave = onSave;
        // UI is initialized via init() to avoid 'this' escaping during construction
    }

    /**
     * Build the UI components. Call this on the EDT before showing the dialog.
     */
    public void init() {
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(24,24,24));
        GridBagConstraints eg = new GridBagConstraints();
        eg.insets = new Insets(6,8,6,8);
        eg.fill = GridBagConstraints.HORIZONTAL;

        eg.gridx = 0; eg.gridy = 0;
        JLabel titleLbl = new JLabel("Title:");
        titleLbl.setForeground(Color.WHITE);
        add(titleLbl, eg);

        JTextField tTitle = new JTextField(task.getTitle(), 20);
        tTitle.setBackground(new Color(45,45,45)); tTitle.setForeground(Color.WHITE);
        eg.gridx = 1; add(tTitle, eg);

        eg.gridx = 0; eg.gridy = 1;
        JLabel startLbl = new JLabel("Start Time (HH:mm):");
        startLbl.setForeground(Color.WHITE);
        add(startLbl, eg);

        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        String startValue = task.getStartTime() != null ? task.getStartTime().format(tf) : "";
        JTextField tStart = new JTextField(startValue, 8);
        tStart.setBackground(new Color(45,45,45)); tStart.setForeground(Color.WHITE);
        eg.gridx = 1; add(tStart, eg);

        eg.gridx = 0; eg.gridy = 2;
        JLabel durLbl = new JLabel("Duration (min):");
        durLbl.setForeground(Color.WHITE);
        add(durLbl, eg);

        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(Math.max(1, task.getDurationMinutes()), 1, 1440, 5));
        JComponent editor = durationSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(new Color(45,45,45));
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(Color.WHITE);
        }
        eg.gridx = 1; add(durationSpinner, eg);

        // Use the static nested StyledBtn (defined below) to avoid capturing outer 'this' during construction

        StyledBtn save = new StyledBtn("Save", new Color(41,128,185));
        save.addActionListener(ev -> {
            try {
                String newTitle = tTitle.getText().trim();
                if (newTitle.isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "Title cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String s = tStart.getText().trim();
                LocalTime newStart = null;
                if (!s.isEmpty()) {
                    newStart = LocalTime.parse(s, tf);
                }
                int newDur = (int) durationSpinner.getValue();
                task.setTitle(newTitle);
                task.setStartTime(newStart);
                task.setEndTime(newStart == null ? null : newStart.plusMinutes(newDur));
                if (onSave != null) onSave.run();
                dispose();
            } catch (DateTimeParseException dtpe) {
                JOptionPane.showMessageDialog(parent, "Start time must be HH:mm", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Invalid data", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        StyledBtn cancel = new StyledBtn("Cancel", new Color(90,90,90));
        cancel.addActionListener(ev -> dispose());

        eg.gridx = 0; eg.gridy = 3; eg.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        btnPanel.setBackground(new Color(24,24,24));
        btnPanel.add(save); btnPanel.add(cancel);
        add(btnPanel, eg);

        pack();
        setResizable(false);
    }

    // Static nested button class avoids 'this' escaping during construction
    public static class StyledBtn extends JButton {
        public static final long serialVersionUID = 1L;

        public StyledBtn(String t, Color bg) {
            super(t);
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 12));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            putClientProperty("bgColor", bg);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = (Color) getClientProperty("bgColor");
            if (bg == null) bg = new Color(41,128,185);
            GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), 0, getHeight(), bg.darker());
            g2d.setPaint(gp);
            g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            g2d.dispose();
            super.paintComponent(g);
        }
    }
}
