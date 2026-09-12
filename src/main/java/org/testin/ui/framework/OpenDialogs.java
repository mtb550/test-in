package org.testin.ui.framework;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * UC-INTERNAL-007, Rule-INTERNAL-075.
 * <p>
 * Which framework dialogs are on screen in this project, so one kind of dialog
 * cannot be opened twice.
 * <p>
 * <b>Why anything has to remember.</b> A framework dialog is a {@link JBPopup},
 * which is not modal, and {@code DialogStyle} deliberately builds it so that
 * neither a click outside nor losing the window dismisses it - Escape cancels,
 * and nothing else does. Nothing stopped a second one: the global search is
 * bound in the keymap, so {@code Ctrl+Alt+F} fires from inside the search dialog
 * that is already open, and the tester ended up with two stacked on each other,
 * each with its own field and its own list (#66, finding 104).
 * <p>
 * <b>Keyed by class, not by project.</b> Two different dialogs may sit on each
 * other - a confirmation over an editor dialog is ordinary - and only a second
 * of the same kind is the mistake.
 * <p>
 * A project service rather than a static map, because a static one is state
 * shared by every open project and {@code tools/inspect.ps1} fails the build for
 * exactly that.
 */
@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public final class OpenDialogs {

    /**
     * One entry per kind of dialog currently on screen. Written and read on the
     * EDT alone - a dialog is shown and closed there and nowhere else.
     */
    private final @NotNull Map<@NotNull Class<?>, @NotNull JBPopup> showing = new HashMap<>();

    /**
     * The dialog of this kind already on screen, and empty when there is none.
     * <p>
     * A popup that has been disposed is not on screen and does not count: the
     * entry outlives the window when a dialog is closed by something that never
     * fires the listener, and a stale entry would refuse every future open.
     */
    @NotNull Optional<JBPopup> shown(final @NotNull Class<?> kind) {
        final @NotNull Optional<JBPopup> open = Optional.ofNullable(showing.get(kind));
        open.filter(JBPopup::isDisposed).ifPresent(gone -> showing.remove(kind));

        return open.filter(popup -> !popup.isDisposed());
    }

    /**
     * Remembers this one until it closes, whichever way it closes - cancelled,
     * submitted or disposed with the project.
     */
    void remember(final @NotNull Class<?> kind, final @NotNull JBPopup popup) {
        showing.put(kind, popup);

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                showing.remove(kind, popup);
            }
        });
    }
}
