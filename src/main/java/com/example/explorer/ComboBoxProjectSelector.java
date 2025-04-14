package com.example.explorer;

import com.example.pojo.Tree;
import com.example.util.sql;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ComboBoxProjectSelector {
    private final ExplorerPanel panel;
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;
    private final java.util.Map<String, Integer> nameToId = new java.util.HashMap<>();

    public ComboBoxProjectSelector(ExplorerPanel panel) {
        this.panel = panel;
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);
        model.addElement("All Projects");

        Tree[] projects = new sql().get("SELECT * FROM tree WHERE type = 0").as(Tree[].class);
        for (Tree project : projects) {
            model.addElement(project.getName());
            nameToId.put(project.getName(), project.getId());
        }

        comboBox.addActionListener(this::onSelection);
    }

    private void onSelection(ActionEvent e) {
        String selected = (String) comboBox.getSelectedItem();
        if ("All Projects".equals(selected)) {
            panel.loadAllProjects();
        } else if (nameToId.containsKey(selected)) {
            panel.filterByProject(nameToId.get(selected));
        }
    }

    public JComponent getComponent() {
        return comboBox;
    }
}
