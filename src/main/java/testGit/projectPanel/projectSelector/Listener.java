package testGit.projectPanel.projectSelector;

import testGit.pojo.tree.dirs.TestProjectDirectory;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Listener implements ActionListener {
    private final ProjectPanel projectPanel;
    private TestProjectDirectory lastSelected = null;

    public Listener(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComboBox<?> comboBox) {
            if (comboBox.getSelectedItem() instanceof TestProjectDirectory selected) {
                if (selected.equals(lastSelected)) {
                    return;
                }

                lastSelected = selected;

                if (projectPanel.getTestProjectSelector() != null) {
                    System.out.println("Selection changed to: " + selected.getName());
                    projectPanel.getTestProjectSelector().filterByTestProject(selected);
                }
            }
        }
    }
}