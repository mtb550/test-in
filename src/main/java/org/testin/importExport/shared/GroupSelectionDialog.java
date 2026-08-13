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
    private final @NotNull JBList<String> list;

    public GroupSelectionDialog(final @NotNull Project p, final @Nullable String currentSelection) {
        super(p, true);
        setTitle("Select Groups");

        final DefaultListModel<String> model = new DefaultListModel<>();
        for (final Group g : Group.values()) {
            model.addElement(g.getName());
        }
        list = new JBList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        if (currentSelection != null && !currentSelection.isBlank()) {
            final List<String> selectedList = Arrays.stream(currentSelection.split(","))
                    .map(String::trim).toList();

            final List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                if (selectedList.contains(model.getElementAt(i))) {
                    indices.add(i);
                }
            }
            list.setSelectedIndices(indices.stream().mapToInt(i -> i).toArray());
        }

        initFrameless();
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        final JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    public @NotNull String getSelectedGroupsStr() {
        return String.join(", ", list.getSelectedValuesList());
    }
}
