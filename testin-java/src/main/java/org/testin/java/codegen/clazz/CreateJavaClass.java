package org.testin.java.codegen.clazz;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestSetDirectoryDto;

import java.util.List;

public class CreateJavaClass implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestSetDirectoryDto dir)) return;
        final @NotNull List<String> fqcn = Fqcn.ofClass(p, dir);
        if (fqcn.isEmpty()) return;

        final @NotNull List<String> packageSegments = fqcn.subList(0, fqcn.size() - 1);
        final @NotNull String className = fqcn.getLast();

        Logger.info("Ready to generate Test Class: " + className + " in package: " + fqcn);

        JavaSourceRoot.writeInRootOrWarn(p, "creating test class",
                root -> JavaSourceRoot.classFile(root, packageSegments, className));
    }
}