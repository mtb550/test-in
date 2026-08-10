package org.testin.Dialogs;

import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.statusBar.StatusBar;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;

@Getter
public class RunOpeningForm {

    private final JBPanel<?> mainPanel;

    private final JBList<TestCaseDto> list;

    private final CollectionListModel<TestCaseDto> model;

    private final JBScrollPane scrollPane;

    public RunOpeningForm(final @NotNull AbstractToolbarPanel toolBar, final @NotNull StatusBar statusBar) {
        this.model = new CollectionListModel<>();
        this.list = new JBList<>(model);

        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading..");
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
    }
}