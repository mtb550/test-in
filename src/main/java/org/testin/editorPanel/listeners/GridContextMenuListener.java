package org.testin.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.testin.editorPanel.AbstractEditorContextMenu;
import org.testin.mappers.dto.TestCaseDto;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GridContextMenuListener extends MouseAdapter {
    private final JBTable table;
    private final JBList<TestCaseDto> list;
    private final AbstractEditorContextMenu contextMenu;
    private final List<TestCaseDto> pageItems;

    public GridContextMenuListener(final JBTable table, final JBList<TestCaseDto> list, final AbstractEditorContextMenu contextMenu, final List<TestCaseDto> pageItems) {
        this.table = table;
        this.list = list;
        this.contextMenu = contextMenu;
        this.pageItems = pageItems;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        if (!SwingUtilities.isRightMouseButton(e)) return;
        final int row = table.rowAtPoint(e.getPoint());
        if (row < 0 || row >= pageItems.size()) return;
        list.setSelectedIndex(row);
        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu)
                .getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}
