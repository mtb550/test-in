package org.testin.viewPanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.settings.StartupActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Getter
    private static ViewPanel viewPanel;

    public static ToolWindow getToolWindow(final @NotNull Project p) {
        return ToolWindowManager.getInstance(p).getToolWindow("testin.view");
    }

    public static void showPanel(final @NotNull Project p, final List<TestCaseDto> testCases, final ArrayList<String> path, final Consumer<ViewPanel> onReadyAction) {
        ToolWindow tw = getToolWindow(p);

        if (tw != null) {
            tw.show(() -> {
                ViewPanel viewer = getViewPanel();
                if (viewer != null) {
                    viewer.show(testCases, path);

                    if (onReadyAction != null) {
                        onReadyAction.accept(viewer);
                    }
                }
            });
        }
    }

    public static void showPanel(final @NotNull Project p, final List<TestCaseDto> testCases, final ArrayList<String> path) {
        showPanel(p, testCases, path, null);
    }

    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow toolWindow) {
        Logger.info("ViewToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) {
                StartupActivity.execute(p);
            }

            viewPanel = new ViewPanel(p);

            ContentFactory contentFactory = ContentFactory.getInstance();

            Content detailsTab = contentFactory.createContent(viewPanel.getDetailsScrollPane(), "Details", false);
            Content historyTab = contentFactory.createContent(viewPanel.getHistoryScrollPane(), "History", false);
            Content bugsTab = contentFactory.createContent(viewPanel.getOpenBugsScrollPane(), "Open Bugs", false);

            toolWindow.getContentManager().addContent(detailsTab);
            toolWindow.getContentManager().addContent(historyTab);
            toolWindow.getContentManager().addContent(bugsTab);

            toolWindow.setTitleActions(new ViewPanelActions().create(viewPanel.getPage(), toolWindow.getComponent()));
            ///toolWindow.setTitle(Bundle.getPluginName());
            ///toolWindow.setStripeTitle(Bundle.getPluginName());

        });
    }

}