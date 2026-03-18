package testGit.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.CreateTestCase;
import testGit.editorPanel.EditorContextMenu;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestMouseListener extends MouseAdapter {
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final EditorContextMenu editorContextMenu;
    private final DefaultActionGroup emptyMenu;

    public TestMouseListener(final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final DirectoryDto dir, final EditorContextMenu editorContextMenu) {
        this.list = list;
        this.model = model;
        this.editorContextMenu = editorContextMenu;
        this.emptyMenu = new DefaultActionGroup();
        this.emptyMenu.add(new CreateTestCase(dir, list, model));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        boolean isClickOnItem = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {

            if (isClickOnItem) {
                TestCaseDto selected = model.getElementAt(index);

                if (selected != null) {
                    ViewPanel.show(selected);
                }
            }
            return;
        }

        if (SwingUtilities.isRightMouseButton(e)) {

            if (isClickOnItem) {
                if (!list.isSelectedIndex(index)) {
                    list.setSelectedIndex(index);
                }

                ActionManager.getInstance()
                        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, editorContextMenu)
                        .getComponent().show(e.getComponent(), e.getX(), e.getY());
            } else {
                list.clearSelection();

                ActionManager.getInstance()
                        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, emptyMenu)
                        .getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}