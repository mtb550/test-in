/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.java.codegen.clazz;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.Fqcn;
import org.testin.codegen.GenAction;
import org.testin.codegen.Renamed;
import org.testin.logger.Logger;
import org.testin.util.NameSanitizer;

import java.util.List;
import java.util.Optional;

public class RenameJavaClass implements GenAction {

    // UC-CODEGEN-015, Rule-CODEGEN-052
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof Renamed renamed)) return;

        final @NotNull String newName = renamed.newName();
        final @NotNull List<String> fqcn = Fqcn.ofClass(renamed.dir());
        if (fqcn.isEmpty()) return;
        final @NotNull String path = String.join(".", fqcn);

        WriteCommandAction.runWriteCommandAction(p, "Rename Test Class", null, () -> {
            final @NotNull Optional<PsiClass> found = Optional.ofNullable(
                    JavaPsiFacade.getInstance(p).findClass(path, GlobalSearchScope.projectScope(p)));
            if (found.isEmpty()) {
                Logger.warn("RenameJavaClass: class not found: " + path);
                return;
            }

            final @NotNull PsiClass targetClass = found.orElseThrow();
            final @NotNull String newClassName = NameSanitizer.className(newName);
            // Receiver is the non-null side: PsiClass#getName is null for anonymous classes.
            if (!newClassName.equals(targetClass.getName())) {
                targetClass.setName(newClassName);
            }
            Logger.info("Renamed test class to: " + newClassName);
        });
    }

}
