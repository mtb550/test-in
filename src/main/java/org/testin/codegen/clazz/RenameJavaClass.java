package org.testin.codegen.clazz;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.util.NameSanitizer;

import java.util.List;

public class RenameJavaClass implements GenAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestSetDirectoryDto dir)) return;
        execute(p, dir, dir.getName());
    }

    public void execute(final @NotNull Project p, final @NotNull TestSetDirectoryDto dir, final @NotNull String newName) {
        final List<String> fqcn = Fqcn.ofClass(p, dir);
        if (fqcn.isEmpty()) return;
        final String path = String.join(".", fqcn);

        WriteCommandAction.runWriteCommandAction(p, "Rename Test Class", null, () -> {
            final PsiClass targetClass = JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p));
            if (targetClass == null) {
                Logger.warn("RenameJavaClass: class not found: " + path);
                return;
            }

            final String newClassName = NameSanitizer.className(newName);
            // Receiver is the non-null side: PsiClass#getName is null for anonymous classes.
            if (!newClassName.equals(targetClass.getName())) {
                targetClass.setName(newClassName);
            }
            Logger.info("Renamed test class to: " + newClassName);
        });
    }

}
