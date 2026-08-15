package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.Group;
import org.testin.ui.dialogs.FramelessDialogWrapper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GroupSelectionDialog extends FramelessDialogWrapper {
    private final @NotNull JBList<Group> list;

    public GroupSelectionDialog(final @NotNull Project p, final @Nullable String currentSelection) {
        super(p);
        setTitle("Select Groups");

        // The list holds the groups and renders their names. Only the value
        // coming in and going out is text, because that is how a test case
        // stores its groups - inside the dialog a group is a group.
        final DefaultListModel<Group> model = new DefaultListModel<>();
        for (final Group g : Group.values()) {
            model.addElement(g);
        }
        list = new JBList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(SimpleListCellRenderer.create("", Group::getName));

        if (currentSelection != null && !currentSelection.isBlank()) {
            final List<String> selectedNames = Arrays.stream(currentSelection.split(","))
                    .map(String::trim).toList();

            final List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < model.getSize(); i++) {
                if (selectedNames.contains(model.getElementAt(i).getName())) {
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

    /**
     * The selection as a test case stores it: the group names, comma separated.
     */
    public @NotNull String getSelectedGroupsStr() {
        return list.getSelectedValuesList().stream()
                .map(Group::getName)
                .collect(Collectors.joining(", "));
    }
}
