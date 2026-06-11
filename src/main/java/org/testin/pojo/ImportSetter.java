package org.testin.pojo;

import com.intellij.openapi.project.Project;
import org.testin.pojo.dto.TestCaseDto;

@FunctionalInterface
public interface ImportSetter {
    void accept(final Project project, final TestCaseDto tc, final String value);
}
