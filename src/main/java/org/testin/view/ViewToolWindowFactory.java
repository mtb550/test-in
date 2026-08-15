package org.testin.view;

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
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.settings.StartupActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Getter
    private static @Nullable ViewPanel viewPanel;

    static void onPanelDisposed(final @NotNull ViewPanel panel) {
        if (viewPanel == panel) {
            viewPanel = null;
        }
    }

    public static @Nullable ToolWindow getToolWindow(final @NotNull Project p) {
        return ToolWindowManager.getInstance(p).getToolWindow("testin.view");
    }

    public static void showPanel(final @NotNull Project p, final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path, final @Nullable Consumer<ViewPanel> onReadyAction) {
        final ToolWindow tw = getToolWindow(p);

        if (tw != null) {
            tw.show(() -> {
                final ViewPanel viewer = getViewPanel();
                if (viewer != null) {
                    viewer.show(testCases, path);

                    if (onReadyAction != null) {
                        onReadyAction.accept(viewer);
                    }
                }
            });
        }
    }

    public static void showPanel(final @NotNull Project p, final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
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

            final ContentFactory contentFactory = ContentFactory.getInstance();

            final Content detailsTab = contentFactory.createContent(viewPanel.getDetailsScrollPane(), "Details", false);
            final Content historyTab = contentFactory.createContent(viewPanel.getHistoryScrollPane(), "History", false);
            final Content bugsTab = contentFactory.createContent(viewPanel.getOpenBugsScrollPane(), "Open Bugs", false);

            toolWindow.getContentManager().addContent(detailsTab);
            toolWindow.getContentManager().addContent(historyTab);
            toolWindow.getContentManager().addContent(bugsTab);

            toolWindow.setTitleActions(new ViewPanelActions().create(viewPanel.getPage(), toolWindow.getComponent()));
        });
    }

}