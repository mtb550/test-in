package org.testin.util.autoGenerator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.util.List;

public class CreateTestProject implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                WriteAction.run(() -> {

                    VirtualFile sourceRoot = Services.getInstance(project, Tools.class).getTestSourceRoot(project);

                    if (sourceRoot != null) {
                        VirtualFile vf = VfsUtil.createDirectoryIfMissing(sourceRoot, fqcn.getFirst());

                        if (vf != null) {
                            Log.debug("Successfully created project package inside Source Root: " + vf.getPath());
                        }
                    } else {
                        Log.warn("No Source Root found in the project. Please mark a directory as 'Sources Root'.");
                    }
                });
            } catch (Exception ex) {
                Log.error("Failed to create project package: " + ex.getMessage());
            }

        }));
    }
}