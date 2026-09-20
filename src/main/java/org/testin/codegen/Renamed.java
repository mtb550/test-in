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

public record Renamed(@NotNull DirectoryDto dir, @NotNull String newName) {
    public @NotNull String newPackage() {
        return NameSanitizer.packageName(newName);
    }

    // UC-TREE-PANEL-011, Rule-CODEGEN-082
    public boolean toTheFilesName(final @NotNull Project p) {
        return dir.getType() == DirectoryType.TP && TestinYml.names(p, newName);
    }

    // Rule-CODEGEN-082
    public boolean movesCode(final @NotNull Project p) {
        return CodeOn.isOn(p) || OptionalPlugin.JAVA.isAvailable() && toTheFilesName(p);
    }

    // UC-CODEGEN-017, Rule-CODEGEN-080
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
