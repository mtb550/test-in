/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FontSync {

    /**
     * Nothing in the plugin is drawn below this, however small the editor font
     * is set. One owner: it was written as a literal twice in here and a third
     * time in light mode's own font class.
     */
    public static final float FLOOR = 8.0f;

    /**
     * Base size this component was last scaled to. Held per component: a single
     * shared value let whichever component updated first consume the change,
     * leaving every other subscriber's children at the old size.
     */
    private static final @NotNull String LAST_BASE_SIZE = "testin.fontSync.lastBaseSize";

    public static float getBaseFontSize() {
        return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();
    }

    /**
     * How a component takes a new font size, given how far it moved: what it
     * does besides having the font set on itself.
     * <p>
     * Said by whoever syncs the component rather than worked out here. This
     * branched on the kind of component and imported the editor's grid builder
     * to resize a table, so {@code ui} depended on a feature above it (#312,
     * A77). A panel's children are scaled by the same amount, which is what
     * most callers want and what the shorter form does.
     */
    @FunctionalInterface
    public interface Refit {
        void by(float delta);
    }

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable) {
        syncWithNativeEditor(p, component, parentDisposable, delta -> applyDeltaRecursively(component, delta));
    }

    public static void syncWithNativeEditor(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Disposable parentDisposable, final @NotNull Refit refit) {
        updateComponentFontSize(component, refit);

        ApplicationManager.getApplication().getMessageBus().connect(parentDisposable).subscribe(EditorColorsManager.TOPIC, (EditorColorsListener) scheme -> updateComponentFontSize(component, refit));

        NativeEditorZoom.ensureWatching();

        attachWheelZoom(p, component, refit);
    }

    // UC-SETTING-011, Rule-SETTING-039
    public static void attachWheelZoom(final @NotNull Project p, final @NotNull JComponent component) {
        attachWheelZoom(p, component, delta -> applyDeltaRecursively(component, delta));
    }

    private static void attachWheelZoom(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Refit refit) {
        component.addMouseWheelListener(e -> {
            if (e.isControlDown() || e.isMetaDown()) {
                zoomGlobalIdeEditors(p, component, refit, e.getWheelRotation() < 0);
                e.consume();
            }
        });
    }

    /**
     * UC-SETTING-011, Rule-SETTING-037.
     * <p>
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

    // UC-SETTING-011, Rule-SETTING-038
    private static void zoomGlobalIdeEditors(final @NotNull Project p, final @NotNull JComponent component, final @NotNull Refit refit, final boolean zoomIn) {
        ApplicationManager.getApplication().invokeLater(() -> {
            final float newSize = Math.clamp(getBaseFontSize() + (zoomIn ? 1.0f : -1.0f), FLOOR, 72.0f);

            applyGlobally(newSize);

            // Outside applyGlobally, which does nothing once the size is at its
            // limit. The indicator still has to appear then, or wheeling past
            // the floor looks like the zoom stopped responding rather than like
            // it has bottomed out.
            updateComponentFontSize(component, refit);
            ZoomIndicatorDialog.show(p, component, newSize);
        });
    }

    private static void updateComponentFontSize(final @NotNull JComponent component, final @NotNull Refit refit) {
        final float newSize = getBaseFontSize();
        ApplicationManager.getApplication().invokeLater(() -> Optional.ofNullable(component.getFont()).ifPresent(currentFont -> {
            // Each subscriber tracks its own last size, so a font change
            // scales every synced component's children - not just the
            // first one the message bus happens to notify.
            // Nothing stored the first time, which instanceof answers; a
            // @NotNull local in between said otherwise (#312, A79).
            final float lastSize = component.getClientProperty(LAST_BASE_SIZE) instanceof Float previous ? previous : newSize;

            final float delta = newSize - lastSize;
            final boolean rootNeedsUpdate = currentFont.getSize2D() != newSize;
            if (delta != 0.0f || rootNeedsUpdate) {
                component.putClientProperty(LAST_BASE_SIZE, newSize);
                component.setFont(currentFont.deriveFont(newSize));
                refit.by(delta);
                component.revalidate();
                component.repaint();
            }
        }));
    }

    private static void applyDeltaRecursively(final @NotNull Container container, final float delta) {
        for (final Component child : container.getComponents()) {
            Optional.ofNullable(child.getFont()).ifPresent(font ->
                    child.setFont(font.deriveFont(Math.max(FLOOR, font.getSize2D() + delta))));

            if (child instanceof Container)
                applyDeltaRecursively((Container) child, delta);
        }
    }
}
