package testGit.editorPanel;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.AddTestCaseAction;
import testGit.pojo.TestCase;

import java.awt.event.MouseEvent;

public class MouseAdapter extends java.awt.event.MouseAdapter {
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final String featurePath;

    public MouseAdapter(JBList<TestCase> list, CollectionListModel<TestCase> model, String featurePath) {
        this.list = list;
        this.model = model;
        this.featurePath = featurePath;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) return;

        int idx = list.locationToIndex(e.getPoint());
        boolean isItemClick = idx >= 0 && list.getCellBounds(idx, idx).contains(e.getPoint());

        DefaultActionGroup group = new DefaultActionGroup();
        if (isItemClick) {
            if (!list.isSelectedIndex(idx)) list.setSelectedIndex(idx);
            group.add(new ContextMenu(featurePath, list, model, model.getElementAt(idx)));
        } else {
            list.clearSelection();
            group.add(new AddTestCaseAction(featurePath, list, model));
        }

        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group)
                .getComponent().show(e.getComponent(), e.getX(), e.getY());
    }
}