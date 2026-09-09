package org.testin.java.codegen.clazz;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.JavaSourceRoot;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.List;

public class RemoveJavaClass implements GenAction {

    // UC-CODEGEN-018, Rule-CODEGEN-059
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof DirectoryDto dir)) return;

        final @NotNull List<String> fqcn = Fqcn.ofClass(dir);
        if (fqcn.isEmpty()) return;

        final @NotNull String packagePath = String.join("/", fqcn.subList(0, fqcn.size() - 1));
        final @NotNull String className = fqcn.getLast();
        final @NotNull String fileName = className + ".java";

        JavaSourceRoot.writeInRoot(p, "removing class", testSourceRoot ->
                JavaSourceRoot.deleteUnder(testSourceRoot, packagePath + "/" + fileName, this));
    }

}
