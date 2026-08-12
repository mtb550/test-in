package org.testin.codegen.pkg;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.io.IOException;
import java.util.List;

public class CreateJavaPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;
        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnPackage(dir);

        WriteAction.run(() -> {
            try {
                VirtualFile testSourceRoot = Services.getInstance(p, Tools.class).getTestSourceRootOrWarn(p);
                if (testSourceRoot == null) {
                    return;
                }

                VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, String.join("/", fqcn));
                if (vf == null) {
                    Logger.error("Could not create package directory: " + String.join("/", fqcn));
                    return;
                }
                Logger.info("Package created physically at: " + vf.getPath());

            } catch (final IOException ex) {
                Logger.info("Error creating package: " + ex.getMessage());
            }
        });
    }
}
