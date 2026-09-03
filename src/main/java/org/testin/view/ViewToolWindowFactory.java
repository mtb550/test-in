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
import java.util.Collection;
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

    /**
     * Tells the details panel that these cases were written, so it re-reads if it
     * is showing one of them.
     * <p>
     * Here rather than at each writer. The panel keeps its own copy of the case,
     * so every path that writes one has to say so - and they did not: the test
     * case dialog and undo told it, the run item dialog and the run grid's cell
     * editor did not, and a tester watched the value they had just typed sit
     * unchanged in the details beside the cell they typed it into.
     * <p>
     * A project whose panel has never been built answers empty and nothing
     * happens, which is why no caller checks first.
     */
    public static void refreshIfShowing(final @NotNull Project p, final @NotNull Collection<TestCaseDto> written) {
        panel(p).ifPresent(view -> view.refreshIfShowing(written));
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

            // In declaration order, which is the order they appear in - a tab
            // added to the enum arrives here without this method changing.
            for (final ViewTab tab : ViewTab.values()) {
                toolWindow.getContentManager().addContent(
                        contentFactory.createContent(tab.paneOf(panel), tab.getDisplayName(), false));
            }

            toolWindow.setTitleActions(new ViewPanelActions().create(panel.getPage(), toolWindow.getComponent()));
        });
    }

}