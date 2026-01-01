package com.example.explorer;

import com.example.pojo.Config;
import com.example.pojo.Directory;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.example.explorer.ExplorerTree.mapProjectToDirectory;

public class ComboBoxProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> model;
    public ExplorerPanel panel;

    public ComboBoxProjectSelector(final ExplorerPanel panel) {
        this.panel = panel;
        model = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);
        loadModel();
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    public void loadModel() {
        File[] dirs = Config.getRootFolder().listFiles(File::isDirectory);

        Directory[] projects = (dirs == null) ? new Directory[0] : Arrays.stream(dirs)
                .map(dir -> {
                    String fullName = dir.getName();
                    String[] parts = fullName.split("_", 4);

                    return new Directory()
                            .setFile(dir)
                            .setFileName(fullName)
                            .setFilePath(Config.getRootFolder().toPath().resolve(fullName))
                            .setType(Integer.parseInt(parts[0]))
                            .setId(Integer.parseInt(parts[1]))
                            .setName(parts[2])
                            .setActive(Integer.parseInt(parts[3]));

                })
                .filter(p -> p.getActive() == 1)
                .toArray(Directory[]::new);

        // Sort alphabetically
        //java.util.Arrays.sort(projects);

        System.out.println("Found projects: " + Arrays.stream(projects).map(Directory::getName).collect(Collectors.joining(", ", "[", "]")));

        if (projects.length > 0) {
            for (Directory project : projects) {
                model.addElement(project);
            }

            comboBox.addActionListener(this::onSelection);
            comboBox.setSelectedIndex(0);

        } else {
            comboBox.addItem(new Directory().setName("No projects found"));
            comboBox.setEnabled(false);
            comboBox.setVisible(true);
        }

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Directory directory) {
                    setText(directory.getName());
                } else {
                    setText("No projects found");
                    comboBox.setVisible(true);
                    comboBox.setEnabled(false);
                }
                return this;
            }
        });


    }

    public void addAndSelectProject(Directory project) {
        if (!comboBox.isEnabled()) {
            comboBox.setEnabled(true);
        }
        model.addElement(project);
        comboBox.setSelectedItem(project); // This triggers focus/selection
        panel.filterByProject(project);

    }

    private void onSelection(ActionEvent e) {
        Directory selected = (Directory) comboBox.getSelectedItem();
        if (selected != null) {
            panel.filterByProject(selected);
            System.out.println("select project: " + selected.getName());
        } else {
            System.out.println("selected is null");
        }
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public void reloadProjects() {
        // مسح العناصر القديمة من القائمة المنسدلة
        model.removeAllElements();

        File[] dirs = Config.getRootFolder().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) { ///  make for parallel
                Directory p = mapProjectToDirectory(dir); // الدالة التي تستخدمها لتحويل المجلد لكائن
                if (p != null && p.getActive() == 1) {
                    model.addElement(p);
                }
            }
        }
    }
}
