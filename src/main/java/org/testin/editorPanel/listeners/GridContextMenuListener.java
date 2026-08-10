package org.testin.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GridContextMenuListener extends MouseAdapter {
    private final JBTable table;
    private final JBList<TestCaseDto> list;
    private final AbstractEditorContextMenu contextMenu;
    private final List<TestCaseDto> pageItems;
    private boolean popupShownOnPress;

    public GridContextMenuListener(final JBTable table, final JBList<TestCaseDto> list, final AbstractEditorContextMenu contextMenu, final List<TestCaseDto> pageItems) {
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

    private void showPopupIfRequested(final MouseEvent e) {
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
