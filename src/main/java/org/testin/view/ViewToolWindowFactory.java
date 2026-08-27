package org.testin.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.setting.StartupActivity;
import org.testin.services.Services;

import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * What a caller with nothing to do afterward passes.
     */
    private static final @NotNull Consumer<ViewPanel> NOTHING_AFTER = viewer -> {
    };

    static void onPanelDisposed(final @NotNull Project p, final @NotNull ViewPanel panel) {
        Services.getInstance(p, ViewPanelHolder.class).release(panel);
    }

    /**
     * This project's panel, once its tool window has built it.
     */
    public static @NotNull Optional<ViewPanel> panel(final @NotNull Project p) {
        return Services.getInstance(p, ViewPanelHolder.class).get();
    }

    /**
     * The view tool window, empty in a project that has never opened it - which
     * is what the platform answers, converted here rather than at five callers.
     */
    public static @NotNull Optional<ToolWindow> toolWindow(final @NotNull Project p) {
        return Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow("testin.view"));
    }

    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path, final @NotNull Consumer<ViewPanel> onReadyAction) {
        toolWindow(p).ifPresent(tw -> tw.show(() -> panel(p).ifPresent(viewer -> {
            viewer.show(testCases, path);
            onReadyAction.accept(viewer);
        })));
    }

    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        showPanel(p, testCases, path, NOTHING_AFTER);
    }

    @Override
    public void createToolWindowContent(final @NotNull Project p, final @NotNull ToolWindow toolWindow) {
        Logger.info("ViewToolWindowFactory.createToolWindowContent()");

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!p.isDisposed()) {
                StartupActivity.execute(p);
            }

            final @NotNull ViewPanel panel = new ViewPanel(p);
            Services.getInstance(p, ViewPanelHolder.class).hold(panel);

            final @NotNull ContentFactory contentFactory = ContentFactory.getInstance();

            final @NotNull Content detailsTab = contentFactory.createContent(panel.getDetailsScrollPane(), ViewTab.DETAILS.getDisplayName(), false);
            final @NotNull Content historyTab = contentFactory.createContent(panel.getHistoryScrollPane(), ViewTab.HISTORY.getDisplayName(), false);
            final @NotNull Content bugsTab = contentFactory.createContent(panel.getOpenBugsScrollPane(), ViewTab.OPEN_BUGS.getDisplayName(), false);

            toolWindow.getContentManager().addContent(detailsTab);
            toolWindow.getContentManager().addContent(historyTab);
            toolWindow.getContentManager().addContent(bugsTab);

            toolWindow.setTitleActions(new ViewPanelActions().create(panel.getPage(), toolWindow.getComponent()));
        });
    }

}