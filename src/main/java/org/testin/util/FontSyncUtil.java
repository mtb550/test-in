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
import org.jetbrains.annotations.NotNull;
import org.testin.ui.ZoomIndicator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public class FontSyncUtil {

    private static boolean isGlobalWatcherActive = false;

    public static float getBaseFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    public static void syncWithNativeEditor(final @NotNull Project project, final JComponent component, final com.intellij.openapi.Disposable parentDisposable) {
        updateComponentFontSize(component);

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable)
                .subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> updateComponentFontSize(component));

        setupGlobalJavaEditorWatcher(project, parentDisposable);

        component.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                zoomGlobalIdeEditors(project, component, e.getWheelRotation() < 0);
                e.consume();
            }
        });
    }

    private static void setupGlobalJavaEditorWatcher(final @NotNull Project project, final Disposable parentDisposable) {
        if (isGlobalWatcherActive) return;
        isGlobalWatcherActive = true;

        IdeEventQueue.getInstance().addPostprocessor(event -> {
            if (event instanceof MouseWheelEvent e && (e.isControlDown() || e.isMetaDown())) {
                Timer timer = new Timer(50, evt -> syncJavaEditorToGlobal(project));
                timer.setRepeats(false);
                timer.start();
            }
            return false;
        }, parentDisposable);
    }

    private static void syncJavaEditorToGlobal(final @NotNull Project project) {
        try {
            if (!project.isDisposed()) {
                Editor activeEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                if (activeEditor != null) {
                    float localSize = activeEditor.getColorsScheme().getEditorFontSize();
                    EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();

                    if (localSize != globalScheme.getEditorFontSize()) {
                        globalScheme.setEditorFontSize(localSize);
                        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                            editor.getColorsScheme().setEditorFontSize(localSize);
                        }
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(EditorColorsManager.TOPIC)
                                .globalSchemeChange(globalScheme);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void zoomGlobalIdeEditors(final @NotNull Project project, final JComponent component, boolean zoomIn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
            float newSize = Math.clamp(getBaseFontSize() + (zoomIn ? 1.0f : -1.0f), 8.0f, 72.0f);

            globalScheme.setEditorFontSize(newSize);
            for (Editor editor : EditorFactory.getInstance().getAllEditors())
                editor.getColorsScheme().setEditorFontSize(newSize);

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(EditorColorsManager.TOPIC)
                    .globalSchemeChange(globalScheme);

            updateComponentFontSize(component);
            ZoomIndicator.show(project, component, newSize);
        });
    }

    private static void updateComponentFontSize(final JComponent component) {
        float newSize = getBaseFontSize();
        ApplicationManager.getApplication().invokeLater(() -> {
            Font currentFont = component.getFont();
            if (currentFont != null && currentFont.getSize2D() != newSize) {
                float delta = newSize - currentFont.getSize2D();
                component.setFont(currentFont.deriveFont(newSize));
                if (component instanceof JList) component.updateUI();
                else applyDeltaRecursively(component, delta);
                component.revalidate();
                component.repaint();
            }
        });
    }

    private static void applyDeltaRecursively(final Container container, float delta) {
        for (Component child : container.getComponents()) {
            final Font f = child.getFont();
            if (f != null)
                child.setFont(f.deriveFont(Math.max(8.0f, f.getSize2D() + delta)));

            if (child instanceof Container)
                applyDeltaRecursively((Container) child, delta);
        }
    }
}
