package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GenAction {
    void execute(final @NotNull Project p, final @NotNull Object obj);
}