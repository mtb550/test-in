package org.testin.importExport.imports;

import com.intellij.openapi.project.Project;
import org.testin.mappers.dto.TestCaseDto;

@FunctionalInterface
public interface ImportSetter {
    void accept(final Project project, final TestCaseDto tc, final String value);
}
