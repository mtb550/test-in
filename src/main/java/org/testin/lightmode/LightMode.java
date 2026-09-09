package org.testin.lightmode;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
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
 * and for the reason its Javadoc gives: the second copy is always the one that
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
     * UC-EDITOR-PANEL-046.
     * <p>
     * Opens light mode on this run - or closes it, if it is this run that is
     * already showing.
     * <p>
     * Every route out of the window ends here, which is why the caller hands in
     * what to do about it rather than repainting itself after the call: Escape,
     * the window's close button and Alt+F4 change the answer without the toolbar
     * being touched, and a button that only redrew when it was clicked would sit
     * pressed over a window that had gone.
     */
    public void toggle(final @NotNull RunEditor editor, final @NotNull Runnable onChange) {
        final boolean wasShowingThisRun = isOpenOn(editor.getParent());

        window.ifPresent(LightModeWindow::close);

        if (!wasShowingThisRun) {
            window = Optional.of(new LightModeWindow(editor, () -> {
                window = Optional.empty();
                onChange.run();
            }));
        }

        // On every path, not only the opening one. Closing runs the lambda the
        // window was built with, which belongs to whichever button opened it -
        // so with the run split across two editors, the button actually pressed
        // was left drawn as pressed over a window that had gone.
        onChange.run();
    }

    /**
     * UC-EDITOR-PANEL-046.
     * <p>
     * Redraws the window if it is this run it is showing, and does nothing at
     * all otherwise - which is every other run editor in the project, and the
     * usual case.
     * <p>
     * <b>A run that has been signed off closes it instead.</b> Completed and
     * Closed are the end: there is no case left to execute, no verdict left to
     * give, and a window offering three of them over a run that has stopped
     * asking is worse than no window. The same question already greys the
     * toolbar button out, and closing here is what un-presses it - the button
     * reads whether the window exists, so the window going is the button
     * changing.
     */
    public void refresh(final @NotNull TestRunDirectoryDto run) {
        if (!run.isStillOpen()) {
            closeIfShowing(run);
            return;
        }

        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::refresh);
    }

    /**
     * The one way a run signed off takes its window down. The toolbar button is
     * told, because the editor holding it is still there to draw it un-pressed.
     */
    private void closeIfShowing(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(LightModeWindow::close);
    }

    /**
     * UC-EDITOR-PANEL-046.
     * <p>
     * Takes the window down when the run editor it is showing closes.
     * <p>
     * Called by the editor from its own {@code dispose}, beside every other
     * thing that editor takes with it. It used to be a {@code Disposer.register}
     * on the editor here, which was wrong twice: the editor is disposed by a
     * direct call rather than through the Disposer, so the registration never
     * once ran - a tab closed with light mode open left the window standing,
     * reading an editor that no longer existed - and registering a child on
     * something the Disposer had never heard of adopted the editor under the
     * application root, where nothing ever removed it. That is the leak the IDE
     * reported on every quit, with Testin named as the plugin to blame (#292).
     * <p>
     * Quietly, and that is the difference from {@link #closeIfShowing}: the
     * button that would be told is part of the editor that is going, so there is
     * nobody left to tell. The window reference is cleared here for the same
     * reason - a quiet close does not run the lambda that clears it.
     */
    public void editorClosing(final @NotNull TestRunDirectoryDto run) {
        window.filter(open -> open.shows(run)).ifPresent(open -> {
            open.closeQuietly();
            window = Optional.empty();
        });
    }

    /**
     * UC-EDITOR-PANEL-046.
     * <p>
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

    @Override
    public void dispose() {
        // The project is going and the editor that would be told is going with
        // it, so this is the one close that announces nothing.
        window.ifPresent(LightModeWindow::closeQuietly);
    }
}
