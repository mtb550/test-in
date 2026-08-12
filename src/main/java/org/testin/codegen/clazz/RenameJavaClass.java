package org.testin.codegen.clazz;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorAction;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Tools;

import java.util.List;

public class RenameJavaClass implements GeneratorAction {

    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestSetDirectoryDto dir)) return;
        execute(p, dir, dir.getName());
    }

    public void execute(final @NotNull Project p, final @NotNull TestSetDirectoryDto dir, final @NotNull String newName) {
        final List<String> fqcn = Services.getInstance(p, Tools.class).buildFqcnClass(p, dir);
        if (fqcn.isEmpty()) return;
        final String path = String.join(".", fqcn);

        WriteCommandAction.runWriteCommandAction(p, "Rename Test Class", null, () -> {
            final PsiClass targetClass = JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p));
            if (targetClass == null) {
                Logger.warn("RenameJavaClass: class not found: " + path);
                return;
            }

            final String newClassName = Services.getInstance(p, Tools.class).sanitizeClassName(newName);
            if (!targetClass.getName().equals(newClassName)) {
                targetClass.setName(newClassName);
            }
            Logger.info("Renamed test class to: " + newClassName);
        });
    }

}
