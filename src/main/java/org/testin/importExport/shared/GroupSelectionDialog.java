package org.testin.importExport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.Group;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupSelectionDialog extends FramelessDialogWrapper {
    private final JBList<String> list;

    public GroupSelectionDialog(final @NotNull Project p, String currentSelection) {
        super(p, true);
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

        initFrameless();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    public String getSelectedGroupsStr() {
        return String.join(", ", list.getSelectedValuesList());
    }
}
