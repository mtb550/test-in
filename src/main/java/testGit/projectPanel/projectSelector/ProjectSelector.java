package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.util.DirectoryMapper;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

public class ProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> model;
    public ProjectPanel projectPanel;

    public ProjectSelector(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.model = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);

        comboBox.setRenderer(new Renderer());
        comboBox.addActionListener(new Listener(projectPanel));

        loadProjectList();
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    public void loadProjectList() {
        System.out.println("ComboBoxProjectSelector.loadProjects()");

        model.removeAllElements();

        File root = Config.getRootFolderFile();
        File[] dirs = root.listFiles(File::isDirectory);

        Directory allProjects = new Directory().setName("All Projects");
        model.addElement(allProjects);

        if (dirs != null) {
            Arrays.stream(dirs)
                    .filter(dir -> !dir.getName().equals(".git") && dir.getName().contains("_"))
                    .map(DirectoryMapper::map)
                    .filter(p -> p != null && p.getActive() == 1)
                    .forEach(model::addElement);
        }

        comboBox.setEnabled(model.getSize() > 0);
        comboBox.setSelectedIndex(0);
    }

    public JComboBox<Directory> selected() {
        return comboBox;
    }

    public void addAndSelectProject(Directory project) {
        System.out.println("ComboBoxProjectSelector.addAndSelectProject()");

        if (!comboBox.isEnabled()) {
            comboBox.setEnabled(true);
        }

        model.addElement(project);
        comboBox.setSelectedItem(project);
    }
}