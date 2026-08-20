package org.testin.editor;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import javax.swing.*;
import java.util.Optional;
import java.awt.*;

/**
 * The editor's swappable middle: the panel, and whichever component is in it.
 * <p>
 * Both editors switch between a list and a grid, and both tracked the component
 * currently in the center with a field of their own so they could take it out
 * again. Forgetting the removal leaves two views stacked in one BorderLayout
 * slot, which is why this holds the pair together rather than leaving the field
 * beside the panel and hoping.
 */
public final class EditorCenter {

    private final @NotNull JBPanel<?> panel;

    /**
     * Whatever is in the center, and nothing before the first {@link #set}.
     */
    private @NotNull Optional<JComponent> current = Optional.empty();

    public EditorCenter(final @NotNull JBPanel<?> panel) {
        this.panel = panel;
    }

    /**
     * Puts this component in the center, taking out whatever was there.
     */
    public void set(final @NotNull JComponent component) {
        Logger.debug("[center] setCenter -> " + component.getClass().getSimpleName()
                + " (had center=" + current.isPresent() + ")");

        current.ifPresent(panel::remove);

        panel.add(component, BorderLayout.CENTER);
        current = Optional.of(component);

        panel.revalidate();
        panel.repaint();
    }
}
