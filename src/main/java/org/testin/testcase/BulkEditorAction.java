package org.testin.testcase;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.function.Consumer;

@FunctionalInterface
public interface BulkEditorAction {
    void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> items, final @NotNull Consumer<List<TestCaseDto>> updatedItems);
}
