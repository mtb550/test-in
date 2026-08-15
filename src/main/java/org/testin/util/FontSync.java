package org.testin.util;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.dialogs.ZoomIndicatorDialog;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.logger.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public class FontSync {

    /**
     * Base size this component was last scaled to. Held per component: a single
     * shared value let whichever component updated first consume the change,
     * leaving every other subscriber's children at the old size.
     */
    private static final String LAST_BASE_SIZE = "testin.fontSync.lastBaseSize";

    private static boolean isGlobalWatcherActive = false;

    public static float getBaseFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable) {
        updateComponentFontSize(component);

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> updateComponentFontSize(component));

        setupGlobalJavaEditorWatcher(p, parentDisposable);

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

    private static void setupGlobalJavaEditorWatcher(final @NotNull Project p, final @NotNull Disposable parentDisposable) {
        if (isGlobalWatcherActive) return;
        isGlobalWatcherActive = true;

        // Single debounce timer instead of allocating one per wheel event.
        final Timer debounce = new Timer(50, evt -> syncJavaEditorToGlobal(p));
        debounce.setRepeats(false);

        IdeEventQueue.getInstance().addPostprocessor(event -> {
            if (event instanceof MouseWheelEvent e && (e.isControlDown() || e.isMetaDown())) {
                debounce.restart();
            }
            return false;
        }, parentDisposable);

        // When the owning editor goes away, let the next editor re-register the watcher.
        Disposer.register(parentDisposable, () -> {
            debounce.stop();
            isGlobalWatcherActive = false;
        });
    }

    private static void syncJavaEditorToGlobal(final @NotNull Project p) {
        try {
            if (!p.isDisposed()) {
                final Editor activeEditor = FileEditorManager.getInstance(p).getSelectedTextEditor();
                if (activeEditor != null) {
                    final float localSize = activeEditor.getColorsScheme().getEditorFontSize();
                    final EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();

                    if (localSize != globalScheme.getEditorFontSize()) {
                        globalScheme.setEditorFontSize(localSize);
                        for (final Editor editor : EditorFactory.getInstance().getAllEditors()) {
                            editor.getColorsScheme().setEditorFontSize(localSize);
                        }
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(EditorColorsManager.TOPIC)
                                .globalSchemeChange(globalScheme);
                    }
                }
            }
        } catch (final Exception ex) {
            Logger.error(ex.getMessage());
        }
    }

    private static void zoomGlobalIdeEditors(final @NotNull Project p, final @NotNull JComponent component, final boolean zoomIn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
            final float newSize = Math.clamp(getBaseFontSize() + (zoomIn ? 1.0f : -1.0f), 8.0f, 72.0f);

            globalScheme.setEditorFontSize(newSize);
            for (final Editor editor : EditorFactory.getInstance().getAllEditors())
                editor.getColorsScheme().setEditorFontSize(newSize);

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(EditorColorsManager.TOPIC)
                    .globalSchemeChange(globalScheme);

            updateComponentFontSize(component);
            ZoomIndicatorDialog.show(p, component, newSize);
        });
    }

    private static void updateComponentFontSize(final @NotNull JComponent component) {
        final float newSize = getBaseFontSize();
        ApplicationManager.getApplication().invokeLater(() -> {
            final Font currentFont = component.getFont();
            if (currentFont != null) {
                // Each subscriber tracks its own last size, so a font change
                // scales every synced component's children - not just the
                // first one the message bus happens to notify.
                final Object stored = component.getClientProperty(LAST_BASE_SIZE);
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
            }
        });
    }

    private static void applyDeltaRecursively(final @NotNull Container container, final float delta) {
        for (final Component child : container.getComponents()) {
            final Font f = child.getFont();
            if (f != null)
                child.setFont(f.deriveFont(Math.max(8.0f, f.getSize2D() + delta)));

            if (child instanceof Container)
                applyDeltaRecursively((Container) child, delta);
        }
    }
}
