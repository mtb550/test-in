package testGit.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.ContextMenu;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.util.ShortcutSet;

import java.awt.*;

public class ShowTestCaseContextMenu extends DumbAwareAction {
    private final Directory dir;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;

    public ShowTestCaseContextMenu(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        // This sets the text for the action if it ever appears in a search or menu
        super("Show Context Menu");
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(ShortcutSet.ContextMenu.get(), list);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        showMenu();
    }

    public void showMenu() {
        int index = list.getSelectedIndex();
        DefaultActionGroup group = new DefaultActionGroup();

        if (index >= 0) {
            Rectangle rect = list.getCellBounds(index, index);
            if (rect == null) return;

            group.add(new ContextMenu(dir, list, model, model.getElementAt(index)));
            showPopup(group, rect.x + (rect.width / 4), rect.y + (rect.height / 2));
        } else {
            group.add(new CreateTestCase(dir, list, model));
            showPopup(group, 10, 10);
        }
    }

    private void showPopup(DefaultActionGroup group, int x, int y) {
        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group)
                .getComponent()
                .show(list, x, y);
    }
}