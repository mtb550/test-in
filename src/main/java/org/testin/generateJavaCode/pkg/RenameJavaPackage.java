package org.testin.generateJavaCode.pkg;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.generateJavaCode.GeneratorAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;

import java.io.IOException;

public class RenameJavaPackage implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;
        final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(dir.getPath());

        if (vf != null)
            WriteAction.run(() -> {
                try {
                    if (vf.exists()) {
                        vf.rename(this, dir.getName());
                        Logger.info("Package renamed to: " + dir.getName());
                    }
                } catch (final IOException ex) {
                    Logger.info("Error renaming package: " + ex.getMessage());
                }
            });

    }

}
