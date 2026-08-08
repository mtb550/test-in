package org.testin.enums;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;

@FunctionalInterface
public interface RunValueExtractor {
    String execute(final TestRunItems item, final @NotNull Project p);
}
