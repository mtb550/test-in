package org.testin.codegen.pkg;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.List;

public class CreateJavaPackage implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;
        final List<String> fqcn = Fqcn.ofPackage(dir);

        JavaSourceRoot.writeInRootOrWarn(p, "creating package", testSourceRoot -> {
            final VirtualFile vf = VfsUtil.createDirectoryIfMissing(testSourceRoot, String.join("/", fqcn));
            if (vf == null) {
                Logger.error("Could not create package directory: " + String.join("/", fqcn));
                return;
            }
            Logger.info("Package created physically at: " + vf.getPath());
        });
    }
}
