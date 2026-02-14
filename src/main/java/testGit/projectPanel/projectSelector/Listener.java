package testGit.projectPanel.projectSelector;

import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Listener implements ActionListener {
    private final ProjectPanel projectPanel;

    public Listener(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComboBox<?> comboBox) {
            Directory selected = (Directory) comboBox.getSelectedItem();
            if (selected != null) {
                System.out.println("Selection changed to: " + selected.getName());
                projectPanel.filterByProject(selected);
            }
        }
    }
}
