package org.testin.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.dialogs.ZoomIndicatorDialog;
import org.testin.editor.grid.GridPanelBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class FontSync {

    /**
     * Base size this component was last scaled to. Held per component: a single
     * shared value let whichever component updated first consume the change,
     * leaving every other subscriber's children at the old size.
     */
    private static final @NotNull String LAST_BASE_SIZE = "testin.fontSync.lastBaseSize";

    public static float getBaseFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable) {
        updateComponentFontSize(component);

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> updateComponentFontSize(component));

        NativeEditorZoom.ensureWatching();

        attachWheelZoom(p, component);
    }

    public static void attachWheelZoom(final @NotNull Project p, final @NotNull JComponent component) {
        component.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                zoomGlobalIdeEditors(p, component, e.getWheelRotation() < 0);
                e.consume();
            }
        });
    }

    /**
     * Puts a font size on the global scheme and on every open editor, and tells
     * the IDE it changed.
     * <p>
     * The one place that does it. Wheel-zooming a Testin panel and following a
     * zoom made in a Java editor both end here, and each had written the four
     * steps out - so the two drifted: only one of them checked whether the size
     * had actually changed, and the other republished the scheme on every wheel
     * tick after the size had already hit its 8pt floor, walking every open
     * editor each time.
     */
    static void applyGlobally(final float newSize) {
        final @NotNull EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
        if (globalScheme.getEditorFontSize() == newSize) return;

        globalScheme.setEditorFontSize(newSize);
        for (final Editor editor : EditorFactory.getInstance().getAllEditors())
            editor.getColorsScheme().setEditorFontSize(newSize);

        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(EditorColorsManager.TOPIC)
                .globalSchemeChange(globalScheme);
    }

    private static void zoomGlobalIdeEditors(final @NotNull Project p, final @NotNull JComponent component, final boolean zoomIn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final float newSize = Math.clamp(getBaseFontSize() + (zoomIn ? 1.0f : -1.0f), 8.0f, 72.0f);

            applyGlobally(newSize);

            // Outside applyGlobally, which does nothing once the size is at its
            // limit. The indicator still has to appear then, or wheeling past
            // the floor looks like the zoom stopped responding rather than like
            // it has bottomed out.
            updateComponentFontSize(component);
            ZoomIndicatorDialog.show(p, component, newSize);
        });
    }

    private static void updateComponentFontSize(final @NotNull JComponent component) {
        final float newSize = getBaseFontSize();
        ApplicationManager.getApplication().invokeLater(() -> {
            Optional.ofNullable(component.getFont()).ifPresent(currentFont -> {
                // Each subscriber tracks its own last size, so a font change
                // scales every synced component's children - not just the
                // first one the message bus happens to notify.
                final @NotNull Object stored = component.getClientProperty(LAST_BASE_SIZE);
                final float lastSize = stored instanceof Float previous ? previous : newSize;

                final float delta = newSize - lastSize;
                final boolean rootNeedsUpdate = currentFont.getSize2D() != newSize;
                if (delta != 0.0f || rootNeedsUpdate) {
                    component.putClientProperty(LAST_BASE_SIZE, newSize);
                    component.setFont(currentFont.deriveFont(newSize));
                    if (component instanceof JBList) {
                        component.updateUI();
                    } else if (component instanceof JBTable table) {
                        GridPanelBuilder.resizeToFont(table);
                    } else {
                        applyDeltaRecursively(component, delta);
                    }
                    component.revalidate();
                    component.repaint();
                }
            });
        });
    }

    private static void applyDeltaRecursively(final @NotNull Container container, final float delta) {
        for (final Component child : container.getComponents()) {
            Optional.ofNullable(child.getFont()).ifPresent(font ->
                    child.setFont(font.deriveFont(Math.max(8.0f, font.getSize2D() + delta))));

            if (child instanceof Container)
                applyDeltaRecursively((Container) child, delta);
        }
    }
}
