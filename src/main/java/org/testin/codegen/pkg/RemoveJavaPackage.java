package org.testin.codegen.pkg;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.List;

public class RemoveJavaPackage implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;

        final @NotNull List<String> fqcn = Fqcn.ofPackage(dir);
        if (fqcn.isEmpty()) return;
        final @NotNull String packagePath = String.join("/", fqcn);

        JavaSourceRoot.writeInRoot(p, "removing package", testSourceRoot ->
                JavaSourceRoot.deleteUnder(testSourceRoot, packagePath, this));
    }

}
