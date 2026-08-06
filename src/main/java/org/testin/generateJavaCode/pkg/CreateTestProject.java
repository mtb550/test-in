package org.testin.generateJavaCode.pkg;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

public class CreateTestProject implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @NotNull Object obj) {
        if (!(obj instanceof TestProjectDirectoryDto tp)) return;

        final String s = Services.getInstance(project, Tools.class).sanitizePackageName(tp.getName());

        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                WriteAction.run(() -> {

                    VirtualFile sourceRoot = Services.getInstance(project, Tools.class).getTestSourceRoot(project);

                    if (sourceRoot != null) {
                        VirtualFile vf = VfsUtil.createDirectoryIfMissing(sourceRoot, s);

                        if (vf != null) {
                            Logger.debug("Successfully created project package inside Source Root: " + vf.getPath());
                        }
                    } else {
                        Logger.warn("No Source Root found in the project. Please mark a directory as 'Sources Root'.");
                    }
                });
            } catch (final Exception ex) {
                Logger.error("Failed to create project package: " + ex.getMessage());
            }

        }));
    }
}