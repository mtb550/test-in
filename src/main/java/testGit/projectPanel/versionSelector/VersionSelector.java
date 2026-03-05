package testGit.projectPanel.versionSelector;

import com.intellij.openapi.ui.ComboBox;
import testGit.pojo.Directory;
import testGit.pojo.Version;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class VersionSelector {
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;
    private final Map<String, Double> nameToVersion = new HashMap<>();

    public VersionSelector(Directory tree) {
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);
        comboBox.setEnabled(true);
        //loadVersions(project);
    }

    private void onSelection(ActionEvent e) {
    }

    private void loadVersions(int projectId) {
        model.removeAllElements();
        nameToVersion.clear();

        Version[] versions = null;

        if (versions.length > 0) {
            for (Version version : versions) {
                String versionStr = String.valueOf(version.getVersion());
                model.addElement(versionStr);
                nameToVersion.put(versionStr, version.getVersion());
            }
            comboBox.setEnabled(true);
            comboBox.addActionListener(this::onSelection);
            comboBox.setSelectedIndex(0);
        } else {
            comboBox.addItem("No versions found");
            comboBox.setEnabled(false);
        }
    }

    public void setProjectId(int projectId) {
        loadVersions(projectId);
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public Double getSelectedVersion() {
        String selected = (String) comboBox.getSelectedItem();
        return nameToVersion.getOrDefault(selected, null);
    }
}
