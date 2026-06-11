package org.testin.util.autoGenerator;

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

import java.io.IOException;
import java.util.List;

public class CreateTestSetPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @Nullable TestCaseDto tc, final @NotNull List<String> fqcn) {
        WriteAction.run(() -> {
            try {
                VirtualFile testSourceRoot = Services.getInstance(project, Tools.class).getTestSourceRoot(project);

                if (testSourceRoot != null) {
                    VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, String.join("/", fqcn));
                    Log.info("Package created physically at: " + vf.getPath());

                } else {
                    Log.info("Could not find Main Source Root in the project modules.");
                }

            } catch (IOException ex) {
                Log.info("Error creating package: " + ex.getMessage());
            }
        });
    }
}