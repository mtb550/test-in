package com.example.explorer;

import com.example.explorer.actions.*;
import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class TestCaseContext extends DefaultActionGroup {

    DefaultActionGroup addGroup = new DefaultActionGroup("➕ Add", true) {
        @Override
        public void update(@NotNull AnActionEvent e) {
            JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;

            boolean enabled = true;

            if (tree != null) {
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    if (userObject instanceof Directory treeItem && treeItem.getType() == NodeType.FEATURE.getCode()) {
                        enabled = false;
                    }
                }
            }

            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    };

    public TestCaseContext() {
    }

    public TestCaseContext(ExplorerPanel panel) {
        super("Test Case Context Menu", true);

        add(new OpenFeatureActionContext(panel.getTestCaseTree()));
        addGroup.add(new AddSuiteAction(panel.getTestCaseTree()));
        addGroup.add(new AddFeatureAction());
        add(addGroup);
        addSeparator();
        add(new DeleteAction(panel));
        add(new RenameAction(panel));
        addSeparator();
        add(new RunAction());

        addSeparator();

        DefaultActionGroup exportGroup = new DefaultActionGroup("📥 Export", true);
        exportGroup.add(new ExportCsvAction());
        exportGroup.add(new ExportHtmlAction());
        exportGroup.add(new ExportExcelAction());
        exportGroup.add(new ExportJsonAction());
        add(exportGroup);

        DefaultActionGroup importGroup = new DefaultActionGroup("📥 Import", true);
        importGroup.add(new ImportCsvAction());
        importGroup.add(new ImportExcelAction());
        importGroup.add(new ImportJsonAction());
        add(importGroup);

        DefaultActionGroup integrationGroup = new DefaultActionGroup("📥 Integrate", true);
        integrationGroup.add(new IntegrateTestRailAction());
        integrationGroup.add(new IntegrateJiraAction());
        integrationGroup.add(new IntegrateAzureAction());
        add(integrationGroup);

        addSeparator();
        add(new OpenOldVersionsAction());
        add(new ViewCommitsAction());
        add(new TestPlansAction());
    }

}
