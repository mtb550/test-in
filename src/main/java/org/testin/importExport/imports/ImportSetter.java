package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;

@FunctionalInterface
public interface ImportSetter {
    void accept(final @NotNull Project p, final TestCaseDto tc, final String value);
}
