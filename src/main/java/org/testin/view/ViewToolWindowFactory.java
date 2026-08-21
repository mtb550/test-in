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

import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;

public class ViewToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * The panel, from the moment the tool window builds it. Static because the
     * tool window is, and null only before that first build - which is why
     * nothing outside asks for the field itself (#71).
     */
    private static @NotNull Optional<ViewPanel> viewPanel = Optional.empty();

    /**
     * What a caller with nothing to do afterward passes.
     */
    private static final @NotNull Consumer<ViewPanel> NOTHING_AFTER = viewer -> {
    };

    static void onPanelDisposed(final @NotNull ViewPanel panel) {
        if (viewPanel.filter(held -> held == panel).isPresent()) viewPanel = Optional.empty();
    }

    /**
     * The panel, once the tool window has built it.
     */
    public static @NotNull Optional<ViewPanel> panel() {
        return viewPanel;
    }

    /**
     * The view tool window, empty in a project that has never opened it - which
     * is what the platform answers, converted here rather than at five callers.
     */
    public static @NotNull Optional<ToolWindow> toolWindow(final @NotNull Project p) {
        return Optional.ofNullable(ToolWindowManager.getInstance(p).getToolWindow("testin.view"));
    }

    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases,
                                 final @NotNull List<String> path, final @NotNull Consumer<ViewPanel> onReadyAction) {
        toolWindow(p).ifPresent(tw -> tw.show(() -> panel().ifPresent(viewer -> {
            viewer.show(testCases, path);
            onReadyAction.accept(viewer);
        })));
    }

    public static void showPanel(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases,
                                 final @NotNull List<String> path) {
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
            viewPanel = Optional.of(panel);

            final @NotNull ContentFactory contentFactory = ContentFactory.getInstance();

            final @NotNull Content detailsTab = contentFactory.createContent(panel.getDetailsScrollPane(), "Details", false);
            final @NotNull Content historyTab = contentFactory.createContent(panel.getHistoryScrollPane(), "History", false);
            final @NotNull Content bugsTab = contentFactory.createContent(panel.getOpenBugsScrollPane(), "Open Bugs", false);

            toolWindow.getContentManager().addContent(detailsTab);
            toolWindow.getContentManager().addContent(historyTab);
            toolWindow.getContentManager().addContent(bugsTab);

            toolWindow.setTitleActions(new ViewPanelActions().create(panel.getPage(), toolWindow.getComponent()));
        });
    }

}