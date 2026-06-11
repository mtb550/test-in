package org.testin.editorPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.*;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EditorCM extends DefaultActionGroup {
    @Getter
    private static final Set<UUID> globalPendingCutIds = new HashSet<>();
    @Getter
    @Setter
    private static boolean globalCutAction = false;
    @Getter
    @Setter
    private static IEditorUI globalSourceEditorUI = null;
    private final Project project;

    public EditorCM(final Project project, final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Editor Context Menu", true);
        this.project = project;

        add(new CreateTestCase(ui, dir, list, model));
        add(createSubGroup("Set Status", AllIcons.General.Filter,
                List.of(
                        new SetStatusPassed(ui, list),
                        new SetStatusFailed(ui, list),
                        new SetStatusBlocked(ui, list),
                        new SetStatusPending(ui, list),
                        new SetStatusUntested(ui, list)
                )
        ));
        add(new ViewDetails(list, dir.getPath()));
        add(new StartExecution(ui.getToolBar().getCallbacks()));
        addSeparator();
        add(new UpdateTestCase(ui, list, dir.getPath()));
        add(new CopyTestCase(list));
        add(new CopyTestCaseNode(list));
        add(new CutTestCaseNode(ui, list));
        add(new PasteTestCaseNode(ui, list));
        add(new RemoveTestCase(project, dir, list, model));
        addSeparator();
        add(new GenerateTestCase(list));
        add(new RunTestCase(list));
        add(new NavigateToCode(list));
        addSeparator();
        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    public static void clearCutState() {
        globalCutAction = false;
        globalPendingCutIds.clear();

        if (globalSourceEditorUI != null && globalSourceEditorUI.getPreferredFocusedComponent() != null)
            globalSourceEditorUI.getPreferredFocusedComponent().repaint();

        globalSourceEditorUI = null;
    }

    private DefaultActionGroup createSubGroup(final String title, final Icon icon, final List<? extends DumbAwareAction> actions) {
        DefaultActionGroup group = new DefaultActionGroup(title, true);
        group.getTemplatePresentation().setIcon(icon);
        for (AnAction action : actions)
            group.add(action);
        return group;
    }

    public void registerShortcuts(final IEditorUI ui, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model, final EditorCM editorCM) {
        new Escape(list);
        new OpenCM(list, editorCM);
        new CreateTestCase(ui, dir, list, model);
        new UpdateTestCase(ui, list, dir.getPath());
        new RemoveTestCase(project, dir, list, model);
        new OpenTestCaseDetails(list, dir.getPath());
        new CloseTestCaseDetails(list);
        new CopyTestCaseDescription(list);
        new NextPageAction(ui, list);
        new PrevPageAction(ui, list);

    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}