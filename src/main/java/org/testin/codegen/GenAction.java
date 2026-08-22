package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@FunctionalInterface
public interface GenAction {
    void execute(final @NotNull Project p, final @NotNull Object obj);

    /**
     * The same, for many at once. One at a time unless a generator can do
     * better - which the test method generator can, because a whole set is one
     * class and the class only needs finding and formatting once.
     */
    default void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        for (final Object item : items) execute(p, item);
    }
}