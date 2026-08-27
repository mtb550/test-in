package org.testin.view;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This project's view panel, from the moment its tool window builds it.
 * <p>
 * One panel per project, because there is one tool window per project. It was
 * a static field, which is one slot for the whole IDE: with two projects open
 * the second to open the view overwrote the first, and every reader after that
 * - the selection listener, the editors, the update aftermath - reached the
 * wrong project's panel. Selecting a case in project A showed it in project B's
 * tool window, and closing B left A's panel unreachable.
 * <p>
 * The static also outlived the project it belonged to, which {@code dispose}
 * had to clear by hand to stop a closed project being held alive. A project
 * service is disposed with its project, so that is no longer anybody's job.
 */
@Service(Service.Level.PROJECT)
public final class ViewPanelHolder {

    private @NotNull Optional<ViewPanel> panel = Optional.empty();

    /**
     * The panel, empty until this project's tool window has built one.
     */
    @NotNull Optional<ViewPanel> get() {
        return panel;
    }

    void hold(final @NotNull ViewPanel built) {
        panel = Optional.of(built);
    }

    /**
     * Forgets the panel, if the one closing is still the one held.
     * <p>
     * Checked rather than cleared outright: a tool window that is closed and
     * reopened builds the new panel before disposing the old one, and clearing
     * outright then threw the live panel away.
     */
    void release(final @NotNull ViewPanel closing) {
        if (panel.filter(held -> held == closing).isPresent()) panel = Optional.empty();
    }
}
