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

package org.testin.rename;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaCode;
import org.testin.codegen.Renamed;
import org.testin.config.TestinYml;
import org.testin.editor.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testproject.SaveTestinYml;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeRename {
    // UC-TREE-PANEL-011, Rule-TREE-PANEL-036, Rule-TREE-PANEL-111
    public static void apply(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull DirectoryDto dir, final @NotNull String newName, final @NotNull Runnable onDone) {
        Services.getInstance(p, TestinEditors.class).close(p, dir);

        final @NotNull Renamed renamed = new Renamed(dir, newName);
        final @NotNull String oldName = dir.getName();

        // Rule-CODEGEN-082
        if (renamed.toTheFilesName(p)) Services.getInstance(p, BoundTestProject.class).follow(oldName, newName);
        JavaCode.of(dir.getType()).getRenamed().execute(p, renamed);

        final @NotNull Path oldPath = dir.getPath();
        final @NotNull Path newPath = oldPath.getParent().resolve(newName);

        Services.getInstance(p, ProjectIndexer.class).renameNode(oldPath, newPath, () -> {
            if (dir instanceof TestProjectDirectoryDto) {
                projectFollows(p, oldName, newName);
                tp.refresh();
            }

            tp.getProjectTree().refresh();
            Logger.info("Success! Renamed to: " + newName);

            onDone.run();
        });
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004, Rule-CODEGEN-080, Rule-CODEGEN-081
    public static boolean refused(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        if (Services.getInstance(p, ProjectIndexer.class).isTaken(dir.getPath().resolveSibling(newName), Optional.of(dir.getPath()))) {
            notifier.softRefuse(p, Refused.ALREADY_EXISTS, newName);
            return true;
        }

        final @NotNull Renamed renamed = new Renamed(dir, newName);
        if (renamed.packageInTheWay(p)) {
            notifier.softRefuse(p, Refused.PACKAGE_TAKEN, renamed.newPackage());
            return true;
        }

        if (DumbService.isDumb(p) && renamed.movesCode(p) && JavaCode.of(dir.getType()).getRenamed().generates()) {
            notifier.softRefuse(p, Refused.WHILE_INDEXING, Bundle.message("dialog.rename.title"));
            return true;
        }

        return false;
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-110, Rule-TREE-PANEL-112
    private static void projectFollows(final @NotNull Project p, final @NotNull String oldName, final @NotNull String newName) {
        Services.getInstance(p, BoundTestProject.class).follow(oldName, newName);
        if (!TestinYml.names(p, oldName)) return;

        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);
        notifier.infoWithActions(p, Bundle.message("rename.config.names.old.title", oldName),
                Bundle.message("rename.config.names.old.message", newName),
                notifier.action(Bundle.message("yml.save.name"), () -> SaveTestinYml.start(p)),
                notifier.action(Bundle.message("rename.config.open"), () -> TestinYml.openInEditor(p)));
    }
}
