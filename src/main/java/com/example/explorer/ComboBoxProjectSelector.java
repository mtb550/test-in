package com.example.explorer;

import com.example.pojo.Projects;
import com.example.util.sql;
import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ComboBoxProjectSelector {
    private final ExplorerPanel panel;
    private final ComboBox<String> comboBox;
    private final DefaultComboBoxModel<String> model;
    private final Map<String, Integer> nameToId = new HashMap<>();

    public ComboBoxProjectSelector(ExplorerPanel panel) {
        this.panel = panel;
        this.model = new DefaultComboBoxModel<>();
        this.comboBox = new ComboBox<>(model);
        comboBox.setFocusable(false);

        //Projects[] projects = new sql().get("SELECT * FROM projects WHERE active = 1 ORDER BY name").as(Projects[].class);
        File testCasesFolder = new File("/home/mtb/IdeaProjects/untitled/TestGit");

// Get directory names as String array
//        String[] projects = testCasesFolder.list((dir, name) ->
//
//                new File(dir, name).isDirectory()
//        );

        File[] dirs = testCasesFolder.listFiles(File::isDirectory);

        Projects[] projects = (dirs == null) ? new Projects[0] : Arrays.stream(dirs)
                .map(dir -> {
                    String[] parts = dir.getName().split("_");

                    // Ensure the split has enough parts to prevent ArrayIndexOutOfBounds
                    Integer id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    Integer active = Integer.parseInt(parts[2]);

                    // Using your @Accessors(chain = true) logic
                    return new Projects()
                            .setId(id)
                            .setName(name)
                            .setActive(active);
                })
                // Optional: Filter for active only to match your old SQL WHERE active = 1
                .filter(p -> p.getActive() != null && p.getActive() == 1)
                .toArray(Projects[]::new);

        if (projects != null) {
            // Sort alphabetically
            //java.util.Arrays.sort(projects);

            // Now you have: ["ibram", "nafath", ...]
            System.out.println("Found projects: " + java.util.Arrays.toString(projects));
        }

        if (projects.length > 0) {
            for (Projects project : projects) {
                model.addElement(project.getName());
                //nameToId.put(project.getName(), project.getId());
            }
            comboBox.addActionListener(this::onSelection);
            comboBox.setSelectedIndex(0);
        } else {
            comboBox.addItem("No projects found");
            comboBox.setEnabled(false);
        }
    }

    private void onSelection(ActionEvent e) {
        String selected = (String) comboBox.getSelectedItem();
        if (selected != null && nameToId.containsKey(selected)) {
            panel.filterByProject(nameToId.get(selected));
        }
    }

    public JComponent getComponent() {
        return comboBox;
    }

    public int getSelectedProjectId() {
        String selected = (String) comboBox.getSelectedItem();
        return nameToId.getOrDefault(selected, -1);
    }
}
