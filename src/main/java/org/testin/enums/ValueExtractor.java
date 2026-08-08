package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ValueExtractor<T> {
    String execute(final T item, final @NotNull Project p);
}
