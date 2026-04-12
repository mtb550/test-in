package testGit.editorPanel.listeners;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import testGit.actions.CreateTestCase;
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.EditorCM;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class TestMouseListener extends MouseAdapter {
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final EditorCM editorCM;
    private final DefaultActionGroup emptyMenu;
    private final Path path;

    public TestMouseListener(final BaseEditorUI ui, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final DirectoryDto dir, final EditorCM editorCM) {
        this.list = list;
        this.path = dir.getPath();
        this.model = model;
        this.editorCM = editorCM;
        this.emptyMenu = new DefaultActionGroup();
        this.emptyMenu.add(new CreateTestCase(ui, dir.getPath(), list, model));
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        final int index = list.locationToIndex(e.getPoint());
        final boolean isClickOnItem = index >= 0 && list.getCellBounds(index, index).contains(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            if (isClickOnItem) {
                Optional.ofNullable(model.getElementAt(index)).ifPresent(selected -> ViewToolWindowFactory.showPanel(Config.getProject(), List.of(selected), path));
            }
            return;
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            final ActionManager actionManager = ActionManager.getInstance();
            final String place = ActionPlaces.TOOLWINDOW_POPUP;

            if (isClickOnItem) {
                if (!list.isSelectedIndex(index)) {
                    list.setSelectedIndex(index);
                }
                actionManager.createActionPopupMenu(place, editorCM).getComponent().show(e.getComponent(), e.getX(), e.getY());
            } else {
                list.clearSelection();
                actionManager.createActionPopupMenu(place, emptyMenu).getComponent().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}