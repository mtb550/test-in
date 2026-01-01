package com.example.explorer;

import com.example.explorer.actions.AddProjectAction;
import com.example.explorer.actions.CollapseAllAction;
import com.example.explorer.actions.ExpandAllAction;
import com.example.explorer.actions.RefreshAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExplorerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ExplorerPanel panel = new ExplorerPanel();
        Content content = ContentFactory.getInstance().createContent(panel.getPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setTitleActions(List.of(contextMenu(panel).getChildren(null)));

        toolWindow.setAutoHide(false);
        //toolWindow.setTitle("TestGit");
        toolWindow.setIcon(AllIcons.Debugger.Db_array);

        //DefaultActionGroup group = new DefaultActionGroup();
        //toolWindow.setTitleActions(List.of(group.getChildren(null)));
    }

    private DefaultActionGroup contextMenu(ExplorerPanel panel) {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new ExpandAllAction(panel));
        group.add(new CollapseAllAction(panel));
        group.addSeparator();
        group.add(new RefreshAction(panel));
        ///group.add(new SettingsAction()); // no need anymore. to be removed with its class
        group.add(new AddProjectAction(panel));

        return group;
    }
}