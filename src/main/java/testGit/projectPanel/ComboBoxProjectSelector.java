package testGit.projectPanel;

import com.intellij.openapi.ui.ComboBox;
import testGit.pojo.Config;
import testGit.pojo.Directory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import static testGit.projectPanel.DirectoryMapper.mapProject;

public class ComboBoxProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> model;
    public ProjectPanel projectPanel;

    public ComboBoxProjectSelector(final ProjectPanel projectPanel) {
        System.out.println("ComboBoxProjectSelector.ComboBoxProjectSelector()");
        this.projectPanel = projectPanel;
        model = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);
        loadProjects();
    }

    public static Directory getSelectedProject() {
        System.out.println("ComboBoxProjectSelector.getSelectedProject(): ");
        System.out.println("Selected Project: " + ((Directory) comboBox.getSelectedItem()).getName());
        return (Directory) comboBox.getSelectedItem();
    }

    public void loadProjects() {
        System.out.println("ComboBoxProjectSelector.loadProjects()");
        System.out.println(Config.getRootFolderFile().getAbsolutePath());

        comboBox.removeAllItems();
        File[] dirs = Config.getRootFolderFile().listFiles(File::isDirectory);
        Arrays.stream(dirs).forEach(System.out::println);

        Directory[] projects = (dirs == null) ? new Directory[0] : Arrays.stream(dirs)
                .filter(dir -> !dir.getName().equals(".git"))
                .map(dir -> {
                    String fullName = dir.getName();
                    String[] parts = fullName.split("_", 4);

                    return new Directory()
                            .setFile(dir)
                            .setFileName(fullName)
                            .setFilePath(Config.getRootFolderFile().toPath().resolve(fullName))
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
            //comboBox.addItem(new Directory().setName("No projects found"));
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
        System.out.println("ComboBoxProjectSelector.addAndSelectProject()");

        if (!comboBox.isEnabled()) {
            comboBox.setEnabled(true);
        }
        model.addElement(project);
        comboBox.setSelectedItem(project); // This triggers focus/selection
        projectPanel.filterByProject(project);

    }

    private void onSelection(ActionEvent e) {
        System.out.println("ComboBoxProjectSelector.onSelection()");

        Directory selected = getSelectedProject();
        if (selected != null) {
            projectPanel.filterByProject(selected);
            System.out.println("select project: " + selected.getName());
        } else {
            System.out.println("selected is null");
        }
    }

    public JComboBox<Directory> selected() {
        System.out.println("ComboBoxProjectSelector.selected()");

        return comboBox;
    }

    public void reloadProjects() {
        System.out.println("ComboBoxProjectSelector.reloadProjects()");

        // مسح العناصر القديمة من القائمة المنسدلة
        model.removeAllElements();

        File[] dirs = Config.getRootFolderFile().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) { ///  make for parallel
                Directory p = mapProject(dir); // الدالة التي تستخدمها لتحويل المجلد لكائن
                if (p != null && p.getActive() == 1) {
                    model.addElement(p);
                }
            }
        }
    }
}
