package org.testin.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GridContextMenuListener extends MouseAdapter {
    private final @NotNull JBTable table;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull AbstractEditorContextMenu contextMenu;
    private final @NotNull List<TestCaseDto> pageItems;
    private boolean popupShownOnPress;

    public GridContextMenuListener(final @NotNull JBTable table, final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu contextMenu, final @NotNull List<TestCaseDto> pageItems) {
        this.table = table;
        this.list = list;
        this.contextMenu = contextMenu;
        this.pageItems = pageItems;
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        popupShownOnPress = e.isPopupTrigger();
        showPopupIfRequested(e);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.isPopupTrigger() && !popupShownOnPress) {
            showPopupIfRequested(e);
        }
        popupShownOnPress = false;
    }

    private void showPopupIfRequested(final @NotNull MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        final int row = table.rowAtPoint(e.getPoint());
        final int column = table.columnAtPoint(e.getPoint());
        if (row < 0 || row >= pageItems.size() || column < 0) return;

        table.changeSelection(row, column, false, false);
        list.setSelectedIndex(row);
        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu)
                .getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}
