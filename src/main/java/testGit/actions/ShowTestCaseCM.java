package testGit.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.EditorContextMenu;
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.Directory;
import testGit.util.KeyboardSet;

import java.awt.*;

public class ShowTestCaseCM extends DumbAwareAction {
    private final Directory dir;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;

    public ShowTestCaseCM(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("Show Context Menu");
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.ContextMenu.getShortcut(), list);

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

            group.add(new EditorContextMenu(dir, list, model));
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