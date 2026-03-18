package testGit.projectPanel.versionSelector;

import com.intellij.openapi.ui.ComboBox;
import testGit.pojo.dto.VersionDto;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class VersionSelector {
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;
    private final Map<String, Double> nameToVersion = new HashMap<>();

    public VersionSelector(TestProjectDirectoryDto testProjectDirectory) {
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

        VersionDto[] versionDtos = null;

        if (versionDtos.length > 0) {
            for (VersionDto versionDto : versionDtos) {
                String versionStr = String.valueOf(versionDto.getVersion());
                model.addElement(versionStr);
                nameToVersion.put(versionStr, versionDto.getVersion());
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
