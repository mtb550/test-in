package org.testin.util.autoGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.logger.Log;

import java.util.List;

public class UpdateTestMethod implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(project, "Create Test Method", null, () -> {
                    try {
                        //todo, to be implemented, learn from #util.autoGenerator.CreateTestMethod
                    } catch (Exception ex) {
                        Log.error("Failed to inject Java method: " + ex.getMessage());
                    }
                }));
    }

}
