package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.CreateTestCase;
import testGit.editorPanel.EditorContextMenu;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.Directory;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestMouseListener extends MouseAdapter {
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final EditorContextMenu editorContextMenu;
    private final DefaultActionGroup emptyMenu;

    public TestMouseListener(final JBList<TestCase> list, final CollectionListModel<TestCase> model, final Directory dir, final EditorContextMenu editorContextMenu) {
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
                TestCase selected = model.getElementAt(index);

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