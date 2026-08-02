package org.testin.util.autoGenerator;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.io.IOException;
import java.util.List;

// todo, is @CreateTestSetPackage and @org.testin.util.autoGenerator.CreateTestProject are same? then merge it to CreateJavaPackage
public class CreateTestSetPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project project, final @NotNull Object obj) {
        if (!(obj instanceof TestSetPackageDirectoryDto dir)) return;
        final List<String> fqcn = Services.getInstance(project, Tools.class).buildFqcnPackage(dir);

        WriteAction.run(() -> {
            try {
                VirtualFile testSourceRoot = Services.getInstance(project, Tools.class).getTestSourceRoot(project);

                if (testSourceRoot != null) {
                    VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, String.join("/", fqcn));
                    Logger.info("Package created physically at: " + vf.getPath());

                } else {
                    Logger.info("Could not find Main Source Root in the project modules.");
                }

            } catch (final IOException ex) {
                Logger.info("Error creating package: " + ex.getMessage());
            }
        });
    }
}