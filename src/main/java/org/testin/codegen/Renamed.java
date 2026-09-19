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

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.NameSanitizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A node and the name it is about to take.
 * <p>
 * A {@link GenAction} is handed one object, and a rename needs two things: the
 * node as it still is, so its generated code can be found where it currently
 * sits, and the name it is becoming. That is why renaming used to go around
 * {@link GenType} and call the generators directly, behind an instanceof chain
 * of its own (#51).
 * <p>
 * The node has not been renamed yet when this is built. The order matters: the
 * Java is renamed first, while the old name is still what finds it.
 */
public record Renamed(@NotNull DirectoryDto dir, @NotNull String newName) {

    /**
     * The package the new name makes - the one answer, for the check below and
     * for the generator that moves the code.
     */
    public @NotNull String newPackage() {
        return NameSanitizer.packageName(newName);
    }

    /**
     * UC-CODEGEN-017, Rule-CODEGEN-080.
     * <p>
     * Whether the automation code already has the package this rename would make,
     * beside the one it renames - a test project renamed to {@code Tests} in a
     * code project that has its own {@code tests} package. Moving the code there
     * fails, and the tree would rename anyway, leaving every generated method
     * under a name nothing looks for.
     * <p>
     * Only when the old package is there too: a colleague's rename already pulled
     * leaves the new package and no old one, and following it is not in the way.
     * Asked of the VFS, in memory, and only where there is code to move: the
     * Java plugin, and a {@code testin.yml} naming the open test project
     * (Rule-CODEGEN-082).
     */
    public boolean packageInTheWay(final @NotNull Project p) {
        if (!DirectoryType.BECOME_JAVA_PACKAGES.contains(dir.getType()) || !CodeOn.isOn(p)) return false;

        final @NotNull List<String> from = Fqcn.ofPackage(dir);
        final @NotNull List<String> to = new ArrayList<>(from);
        to.set(to.size() - 1, newPackage());
        if (to.equals(from)) return false;

        return JavaSourceRoot.find(p)
                .filter(root -> JavaSourceRoot.under(root, String.join("/", from)).isPresent())
                .flatMap(root -> JavaSourceRoot.under(root, String.join("/", to)))
                .isPresent();
    }
}
