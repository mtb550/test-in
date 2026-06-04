package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.util.reports.TestRunReport;

import javax.swing.tree.DefaultMutableTreeNode;

// TODO: implement save as to allow tester to specify save place
public class GenerateReportXml extends DumbAwareAction {
    private final SimpleTree tree;

    public GenerateReportXml(final SimpleTree tree) {
        super("As XML", "Generate test run XML report", AllIcons.FileTypes.Xml);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof TestRunDirectoryDto tr) {
            final Project project = e.getProject();
            new TestRunReport(project, tr).build().asXml();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        e.getPresentation().setEnabled(selectedNode != null && selectedNode.getUserObject() instanceof TestRunDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}