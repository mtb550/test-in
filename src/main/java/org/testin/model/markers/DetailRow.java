package org.testin.model.markers;

import org.jetbrains.annotations.NotNull;

/**
 * One line a marker has to say about its node: a caption and what goes beside
 * it.
 * <p>
 * Plain text on purpose. A marker is model, and the Details popup is one reader
 * of this - the reports are another - so what a marker hands over is words,
 * never a component.
 */
public record DetailRow(@NotNull String caption, @NotNull String value) {
}
