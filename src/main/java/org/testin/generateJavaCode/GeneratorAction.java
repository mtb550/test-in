package org.testin.generateJavaCode;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GeneratorAction {
    void execute(final @NotNull Project project, final @NotNull Object obj);
}