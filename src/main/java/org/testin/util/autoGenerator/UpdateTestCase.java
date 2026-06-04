package org.testin.util.autoGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.Config;
import com.intellij.openapi.project.Project;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.logger.Log;

import java.util.List;

public class UpdateTestCase implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        ApplicationManager.getApplication().invokeLater(() ->
                WriteCommandAction.runWriteCommandAction(project, "Create Test Method", null, () -> {
                    try {

                    } catch (Exception ex) {
                        Log.error("[ERROR] Failed to inject Java method: " + ex.getMessage());
                    }
                }));
    }

}
