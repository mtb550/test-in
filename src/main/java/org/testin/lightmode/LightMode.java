package org.testin.lightmode;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import java.util.Optional;

/**
 * Whether light mode is open, and the one window it opens (#13).
 * <p>
 * <b>Nothing else remembers.</b> A run editor's button asks
 * {@link #isOpenOn} rather than holding a flag of its own, so Escape, the
 * window's own close button and the project closing all un-press it without
 * three handlers agreeing to. That is the rule
 * {@code RunEditor.onExecutionStateChanged} already follows for Start and Stop,
 * and for the reason its javadoc gives: the second copy is always the one that
 * drifts.
 * <p>
 * <b>One window, and it knows whose run it shows.</b> Several run editors are
 * open at once in the usual case, so "is light mode on?" is not a question the
 * project can answer for all of them - only the editor whose run is in the
 * window is in light mode, and the others must not say they are. Opening it
 * from a second run moves the window to that run.
 * <p>
 * Disposal costs nothing here. A project-level service that is
 * {@link Disposable} is disposed when the project closes, so the window goes
 * with it - no listener to register and none to forget to unregister.
 */
@Service(Service.Level.PROJECT)
public final class LightMode implements Disposable {

    private @NotNull Optional<LightModeWindow> window = Optional.empty();

    /**
     * Opens light mode on this run - or closes it, if it is this run that is
     * already showing.
     * <p>
     * Every route out of the window ends here, which is why the caller hands in
     * what to do about it rather than repainting itself after the call: Escape,
     * the window's close button and Alt+F4 change the answer without the toolbar
     * being touched, and a button that only redrew when it was clicked would sit
     * pressed over a window that had gone.
     */
    public void toggle(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull Runnable onChange) {
        final boolean wasShowingThisRun = isOpenOn(editor.getParent());

        close();

        if (wasShowingThisRun) return;

        window = Optional.of(new LightModeWindow(p, editor, () -> {
            window = Optional.empty();
            onChange.run();
        }));

        onChange.run();
    }

    /**
     * Redraws the window if it is this run it is showing, and does nothing at
     * all otherwise - which is every other run editor in the project, and the
     * usual case.
     */
    public void refresh(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::refresh);
    }

    /**
     * Moves the clocks on, once a second, and touches nothing else - a full
     * refresh re-measures and re-sizes the window, which is not something to do
     * to a tester every second while they are reading.
     */
    public void tick(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::tick);
    }

    public boolean isOpenOn(final @NotNull TestRunDirectoryDto run) {
        return window.filter(open -> open.shows(run)).isPresent();
    }

    public void close() {
        window.ifPresent(LightModeWindow::close);
    }

    @Override
    public void dispose() {
        // The project is going and the editor that would be told is going with
        // it, so this is the one close that announces nothing.
        window.ifPresent(LightModeWindow::closeQuietly);
        window = Optional.empty();
    }
}
