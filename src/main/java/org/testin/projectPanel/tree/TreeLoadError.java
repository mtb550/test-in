package org.testin.projectPanel.tree;

import org.jetbrains.annotations.NotNull;

/**
 * Value rendered when a directory cannot load its children.
 */
public record TreeLoadError(@NotNull String message) {
}
