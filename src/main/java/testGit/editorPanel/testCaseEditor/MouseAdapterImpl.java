package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.CreateTestCase;
import testGit.pojo.Directory;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseAdapterImpl extends MouseAdapter {
    private final JBList<TestCaseJsonMapper> list;
    private final CollectionListModel<TestCaseJsonMapper> model;
    private final Directory dir;

    public MouseAdapterImpl(JBList<TestCaseJsonMapper> list, CollectionListModel<TestCaseJsonMapper> model, Directory dir) {
        this.list = list;
        this.model = model;
        this.dir = dir;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            handleDoubleClick(e);
        }

        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void handleDoubleClick(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
            TestCaseJsonMapper selected = model.getElementAt(index);
            if (selected != null) {
                ViewPanel.show(selected);
            }
        }
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int idx = list.locationToIndex(e.getPoint());
        DefaultActionGroup group = new DefaultActionGroup();

        if (idx >= 0 && list.getCellBounds(idx, idx).contains(e.getPoint())) {
            if (!list.isSelectedIndex(idx)) list.setSelectedIndex(idx);
            group.add(new ContextMenu(dir, list, model, model.getElementAt(idx)));

        } else {
            list.clearSelection();
            group.add(new CreateTestCase(dir, list, model));
        }

        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group)
                .getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}