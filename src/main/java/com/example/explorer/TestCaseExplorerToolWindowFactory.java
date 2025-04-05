package com.example.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TestCaseExplorerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        TestCaseExplorerPanel explorerPanel = new TestCaseExplorerPanel();

        Content content = ContentFactory.getInstance()
                .createContent(explorerPanel.getPanel(), "", false);
        toolWindow.getContentManager().addContent(content);

        // ✅ تولبار يظهر في شريط العنوان مثل Project View
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.expandAll();
            }
        });

        actionGroup.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.collapseAll();
            }
        });

        actionGroup.addSeparator();

        actionGroup.add(new AnAction("Refresh", "Reload", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                explorerPanel.refresh();
            }
        });

        actionGroup.add(new AnAction("Settings", "Configure Tree", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Messages.showMessageDialog("Settings coming soon!", "Info", AllIcons.General.InformationDialog);
            }
        });

        // ✨ هنا نضيف الأزرار إلى العنوان
        toolWindow.setTitleActions(List.of(actionGroup.getChildren(null)));

    }
}
