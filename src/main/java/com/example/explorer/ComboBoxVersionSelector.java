package com.example.explorer;

import com.example.pojo.Version;
import com.example.util.sql;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class ComboBoxVersionSelector {
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;
    private final Map<String, Double> nameToVersion = new HashMap<>();

    public ComboBoxVersionSelector(int projectId) {
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);

        Version[] versions = new sql().get(
                "SELECT version, latest FROM nafath_version WHERE project_id = ? ORDER BY version DESC",
                projectId
        ).as(Version[].class);

        if (versions.length > 0) {
            for (Version version : versions) {
                String versionStr = String.valueOf(version.getVersion());
                model.addElement(versionStr);
                nameToVersion.put(versionStr, version.getVersion());
            }
            comboBox.addActionListener(this::onSelection);
            comboBox.setSelectedIndex(0);
        } else {
            comboBox.addItem("No versions found");
            comboBox.setEnabled(false);
        }
    }

    private void onSelection(ActionEvent e) {
        // For future enhancements
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public Double getSelectedVersion() {
        String selected = (String) comboBox.getSelectedItem();
        return nameToVersion.getOrDefault(selected, null);
    }
}
