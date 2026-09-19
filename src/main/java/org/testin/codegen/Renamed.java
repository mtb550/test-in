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
import org.testin.config.TestinYml;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.OptionalPlugin;
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
     * UC-TREE-PANEL-011, Rule-CODEGEN-082.
     * <p>
     * Whether this renames a test project to the name {@code testin.yml} gives -
     * Ctrl+Z on a rename of the file's project. Code is on for a project only
     * while the file names the open one, so the rename's code moves under the
     * new name, with the choice following first. Asked under the old name, code
     * was off, and the package stayed where the rename had put it while the
     * folder went back (#66, finding 311).
     */
    public boolean toTheFilesName(final @NotNull Project p) {
        return dir.getType() == DirectoryType.TP && TestinYml.names(p, newName);
    }

    /**
     * Rule-CODEGEN-082.
     * <p>
     * Whether this rename moves automation code: code is on now, or turns on as
     * a test project takes the name {@code testin.yml} gives.
     */
    public boolean movesCode(final @NotNull Project p) {
        return CodeOn.isOn(p) || OptionalPlugin.JAVA.isAvailable() && toTheFilesName(p);
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
     * Asked of the VFS, in memory, and only where there is code to move
     * ({@link #movesCode}).
     */
    public boolean packageInTheWay(final @NotNull Project p) {
        if (!DirectoryType.BECOME_JAVA_PACKAGES.contains(dir.getType()) || !movesCode(p)) return false;

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
