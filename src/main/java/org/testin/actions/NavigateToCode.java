package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.CodeNavigator;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;

import java.util.ArrayList;
import java.util.List;

public class NavigateToCode extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public NavigateToCode(final JBList<TestCaseDto> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.NavigateToCode.getCustomShortcut(), list);
    }

    public void execute(final TestCaseDto tc) {
        if (tc == null) return;

        Project project = Config.getProject();
        List<String> navFqcn = new ArrayList<>();

        VirtualFile testRoot = Tools.getInstance().getTestSourceRoot(project);

        if (testRoot != null) {
            navFqcn.add(testRoot.getPath());
        } else {
            System.out.println("[WARNING] No test source root found. Falling back to default.");
            navFqcn.add(project.getBasePath() + "/src/test/java");
        }

        navFqcn.addAll(tc.getFqcn());

        System.out.println("[TRACE] Navigating to: " + tc.getDescription());
        System.out.println("[TRACE] fqcn path mapping: " + navFqcn);

        new CodeNavigator().toCode(navFqcn, tc.getDescription());
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        execute(list.getSelectedValue());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}