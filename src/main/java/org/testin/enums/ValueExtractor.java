package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ValueExtractor<T> {
    @NotNull String execute(final @NotNull T item, final @NotNull Project p);
}
