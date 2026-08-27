package org.testin.util;

import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;

import javax.swing.*;
import java.awt.event.MouseWheelEvent;
import java.util.Optional;

/**
 * Follows a zoom the tester makes in one of the IDE's own editors, and pushes
 * the new size out to the global scheme so the Testin panels follow it.
 * <p>
 * The IDE zooms only the editor under the mouse and leaves the global scheme
 * alone, so nothing on {@code EditorColorsManager.TOPIC} fires and a Testin
 * grid sitting beside a Java editor stayed at the old size. Publishing the
 * change is what this exists to do.
 * <p>
 * One watcher for the IDE, owned by the application, and it acts on the editor
 * the wheel event happened over. It used to be set up by whichever editor
 * opened first, guarded by a static boolean, and it captured that editor's
 * project - so with two projects open the second registered nothing, and a zoom
 * in it read the <em>first</em> project's selected editor and pushed that size
 * everywhere, snapping every editor in both projects to a size nobody had asked
 * for. Closing the owning editor cleared the flag and left no watcher at all
 * until some later editor happened to re-register one.
 * <p>
 * Reading the event's own editor also fixes a smaller thing: it was the
 * project's <em>selected</em> editor before, which in a split window is not
 * necessarily the one being zoomed.
 */
@Service(Service.Level.APP)
public final class NativeEditorZoom implements Disposable {

    /**
     * One timer rather than one per wheel event. A wheel gesture arrives as a
     * burst, and applying each tick means walking every open editor and
     * publishing a scheme change for each one.
     */
    private final @NotNull Timer debounce = new Timer(50, event -> push());

    private @NotNull Optional<Editor> wheeled = Optional.empty();

    NativeEditorZoom() {
        debounce.setRepeats(false);

        IdeEventQueue.getInstance().addPostprocessor(event -> {
            if (event instanceof MouseWheelEvent wheel && (wheel.isControlDown() || wheel.isMetaDown())) {
                // Only when it landed on an editor. A wheel over a Testin panel
                // is that panel's own business - FontSync.attachWheelZoom
                // handles it - and must not disturb a pending push.
                editorUnder(wheel).ifPresent(editor -> {
                    wheeled = Optional.of(editor);
                    debounce.restart();
                });
            }

            return false;
        }, this);
    }

    /**
     * Makes sure the watcher is running. Asking for the service builds it, and
     * building it is what registers the watch, so callers say what they want
     * rather than tracking whether somebody already did this.
     */
    public static void ensureWatching() {
        Services.getInstance(NativeEditorZoom.class);
    }

    private void push() {
        try {
            wheeled.filter(editor -> !editor.isDisposed())
                    .ifPresent(editor -> FontSync.applyGlobally(editor.getColorsScheme().getEditorFontSize()));
        } catch (final Exception ex) {
            Logger.error("Following the editor zoom failed: " + ex.getMessage());
        }
    }

    /**
     * The editor the wheel turned over, and empty when it was anything else.
     */
    private static @NotNull Optional<Editor> editorUnder(final @NotNull MouseWheelEvent wheel) {
        return Optional.ofNullable(wheel.getComponent())
                .map(component -> DataManager.getInstance().getDataContext(component))
                .map(CommonDataKeys.EDITOR::getData);
    }

    @Override
    public void dispose() {
        debounce.stop();
        wheeled = Optional.empty();
    }
}
