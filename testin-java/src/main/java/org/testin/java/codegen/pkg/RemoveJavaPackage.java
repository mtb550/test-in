package org.testin.java.codegen.pkg;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.model.dto.dirs.DirectoryDto;


public class RemoveJavaPackage implements GenAction {

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;

        // Never empty: Fqcn.ofPackage answers "generated" for a node with no
        // packages above it, so there is nothing to guard against here.
        final @NotNull String packagePath = String.join("/", Fqcn.ofPackage(dir));

        JavaSourceRoot.writeInRoot(p, "removing package", testSourceRoot ->
                JavaSourceRoot.deleteUnder(testSourceRoot, packagePath, this));
    }

}
