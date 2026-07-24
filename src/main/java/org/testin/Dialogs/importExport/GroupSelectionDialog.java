package org.testin.Dialogs.importExport;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Group;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupSelectionDialog extends DialogWrapper {
    private final JBList<String> list;

    public GroupSelectionDialog(Project project, String currentSelection) {
        super(project, true);
        setTitle("Select Groups");

        DefaultListModel<String> model = new DefaultListModel<>();
        for (Group g : Group.values()) {
            model.addElement(g.getName());
        }
        list = new JBList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        if (currentSelection != null && !currentSelection.isBlank()) {
            List<String> selectedList = Arrays.stream(currentSelection.split(","))
                    .map(String::trim).toList();

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                if (selectedList.contains(model.getElementAt(i))) {
                    indices.add(i);
                }
            }
            list.setSelectedIndices(indices.stream().mapToInt(i -> i).toArray());
        }

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    public String getSelectedGroupsStr() {
        return String.join(", ", list.getSelectedValuesList());
    }
}
