package com.example.actions;

import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.EventObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsAction extends AnAction {
    private final Map<Integer, ProjectRow> originalProjects = new LinkedHashMap<>();
    private final Map<Integer, UserRow> originalUsers = new LinkedHashMap<>();

    public SettingsAction() {
        super("Settings", "Configure tree", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        JFrame frame = new JFrame("Settings");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);

        JBTabbedPane tabbedPane = new JBTabbedPane();

        // --- Projects Tab ---
        JBPanel<?> projectPanel = new JBPanel<>(new BorderLayout());
        DefaultTableModel projectModel = new DefaultTableModel(new String[]{"ID", "Name", "Active"}, 0);
        JBTable projectTable = new JBTable(projectModel) {
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2;
            }
        };

        projectTable.getColumnModel().getColumn(2).setCellRenderer(new ToggleRenderer());
        projectTable.getColumnModel().getColumn(2).setCellEditor(new ToggleEditor());

        JBScrollPane projectScroll = new JBScrollPane(projectTable);
        projectPanel.add(projectScroll, BorderLayout.CENTER);

        JButton saveProjects = new JButton("Save");
        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        savePanel.add(saveProjects);
        projectPanel.add(savePanel, BorderLayout.SOUTH);

        tabbedPane.add("Projects", projectPanel);

        // --- Users Tab ---
        JBPanel<?> userPanel = new JBPanel<>(new BorderLayout());
        DefaultTableModel userModel = new DefaultTableModel(new String[]{"ID", "Name", "Role", "Email", "Enabled"}, 0);
        JBTable userTable = new JBTable(userModel) {
            public boolean isCellEditable(int row, int column) {
                return column != 0;
            }
        };

        userTable.getColumnModel().getColumn(4).setCellRenderer(new ToggleRenderer());
        userTable.getColumnModel().getColumn(4).setCellEditor(new ToggleEditor());

        JBScrollPane userScroll = new JBScrollPane(userTable);
        userPanel.add(userScroll, BorderLayout.CENTER);

        JButton saveUsers = new JButton("Save");
        JPanel saveUserPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveUserPanel.add(saveUsers);
        userPanel.add(saveUserPanel, BorderLayout.SOUTH);

        tabbedPane.add("Users", userPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);

        // Load data
        loadProjects(projectTable);
        loadUsers(userTable);

        // Save changed projects
        saveProjects.addActionListener(evt -> {
            DefaultTableModel model = (DefaultTableModel) projectTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object idObj = model.getValueAt(i, 0);
                String name = String.valueOf(model.getValueAt(i, 1));
                int active = (boolean) model.getValueAt(i, 2) ? 1 : 0;

                if (name == null || name.isBlank()) continue;

                if (idObj == null || idObj.toString().isBlank()) {
                    // New record
                    new sql().execute("INSERT INTO projects (name, active) VALUES (?, ?)", name, active);
                } else {
                    int id = Integer.parseInt(idObj.toString());
                    ProjectRow original = originalProjects.get(id);
                    if (original != null && (!original.name.equals(name) || original.active != active)) {
                        new sql().execute("UPDATE projects SET name = ?, active = ? WHERE project_id = ?", name, active, id);
                    }
                }
            }
            JOptionPane.showMessageDialog(null, "Projects saved.");
            loadProjects(projectTable);
        });

        // Save changed users
        saveUsers.addActionListener(evt -> {
            DefaultTableModel model = (DefaultTableModel) userTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object idObj = model.getValueAt(i, 0);
                String name = String.valueOf(model.getValueAt(i, 1));
                String role = String.valueOf(model.getValueAt(i, 2));
                String email = String.valueOf(model.getValueAt(i, 3));
                int enabled = (boolean) model.getValueAt(i, 4) ? 1 : 0;

                if (name.isBlank() || email.isBlank()) continue;

                if (idObj == null || idObj.toString().isBlank()) {
                    new sql().execute("INSERT INTO users (name, role, email, enabled) VALUES (?, ?, ?, ?)", name, role, email, enabled);
                } else {
                    int id = Integer.parseInt(idObj.toString());
                    UserRow original = originalUsers.get(id);
                    if (original != null && (!original.name.equals(name) || !original.role.equals(role)
                            || !original.email.equals(email) || original.enabled != enabled)) {
                        new sql().execute("UPDATE users SET name = ?, role = ?, email = ?, enabled = ? WHERE id = ?",
                                name, role, email, enabled, id);
                    }
                }
            }
            JOptionPane.showMessageDialog(null, "Users saved.");
            loadUsers(userTable);
        });
    }

    private void loadProjects(JTable table) {
        originalProjects.clear();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        sql db = new sql().get("SELECT project_id, name, active FROM projects");
        for (HashMap<String, Object> row : db.dbResult) {
            int id = (int) row.get("project_id");
            String name = row.get("name").toString();
            boolean active = Integer.parseInt(row.get("active").toString()) == 1;
            model.addRow(new Object[]{id, name, active});
            originalProjects.put(id, new ProjectRow(name, active ? 1 : 0));
        }

        // Add empty row for new entry
        model.addRow(new Object[]{null, "", Boolean.TRUE});

        table.setAutoCreateRowSorter(true);
    }

    private void loadUsers(JTable table) {
        originalUsers.clear();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        sql db = new sql().get("SELECT id, name, role, email, enabled FROM users");

        for (HashMap<String, Object> row : db.dbResult) {
            int id = (int) row.get("id");
            String name = row.get("name").toString();
            String role = row.get("role").toString();
            String email = row.get("email").toString();
            boolean enabled = Integer.parseInt(row.get("enabled").toString()) == 1;
            model.addRow(new Object[]{id, name, role, email, enabled});
            originalUsers.put(id, new UserRow(name, role, email, enabled ? 1 : 0));
        }
        model.addRow(new Object[]{null, "", "", "", Boolean.TRUE});
        table.setAutoCreateRowSorter(true);
    }

    private static class ProjectRow {
        String name;
        int active;

        ProjectRow(String name, int active) {
            this.name = name;
            this.active = active;
        }
    }

    private static class UserRow {
        String name, role, email;
        int enabled;

        UserRow(String name, String role, String email, int enabled) {
            this.name = name;
            this.role = role;
            this.email = email;
            this.enabled = enabled;
        }
    }

    private static class ToggleRenderer extends JCheckBox implements TableCellRenderer {
        public ToggleRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            return this;
        }
    }

    private static class ToggleEditor extends DefaultCellEditor {
        private final JCheckBox checkBox;

        public ToggleEditor() {
            super(new JCheckBox());
            checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            checkBox.setSelected(Boolean.TRUE.equals(value));
            return checkBox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            return true;
        }
    }
}
