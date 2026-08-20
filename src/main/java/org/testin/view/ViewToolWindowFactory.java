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
import org.jetbrains.annotations.Nullable;
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
    private static @Nullable ViewPanel viewPanel;

    /**
     * What a caller with nothing to do afterward passes.
     */
    private static final @NotNull Consumer<ViewPanel> NOTHING_AFTER = viewer -> {
    };

    static void onPanelDisposed(final @NotNull ViewPanel panel) {
        if (viewPanel == panel) {
            viewPanel = null;
        }
    }

    /**
     * The panel, once the tool window has built it.
     */
    public static @NotNull Optional<ViewPanel> panel() {
        return Optional.ofNullable(viewPanel);
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