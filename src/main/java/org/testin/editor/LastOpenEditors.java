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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class LastOpenEditors {
    private static final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    // Rule-EDITOR-PANEL-015
    public void remember(final @NotNull Project p) {
        try {
            final @NotNull List<String> entries = pathsOfOpen(p);

            PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY,
                    entries.isEmpty() ? null : String.join(";", entries));

        } catch (final Exception ex) {
            Logger.error("Failed to save open editors: " + ex.getMessage());
        }
    }

    private @NotNull List<String> pathsOfOpen(final @NotNull Project p) {
        final @NotNull List<String> entries = new ArrayList<>();

        for (final Path path : Services.getInstance(p, TestinEditors.class).openNodePaths(p)) {
            entries.add(path.toString());
        }

        return entries;
    }

    // UC-INTERNAL-002
    public void reopen(final @NotNull Project p) {
        try {
            final @NotNull String saved = Objects.requireNonNullElse(
                    PropertiesComponent.getInstance(p).getValue(OPEN_EDITORS_KEY), "");

            if (saved.isEmpty()) {
                Logger.debug("No saved editors to restore");
                return;
            }

            final String @NotNull [] entries = saved.split(";");
            if (entries.length == 0) return;

            Logger.info("restoring " + entries.length + " open editors");

            openAll(p, stillIndexed(p, entries));

            PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY, null);
            Logger.info("cleared saved editor state");

        } catch (final Exception ex) {
            Logger.error("Failed to restore open editors: " + ex.getMessage());
        }
    }

    private @NotNull List<DirectoryDto> stillIndexed(final @NotNull Project p, final String @NotNull [] entries) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull List<DirectoryDto> found = new ArrayList<>();

        for (final String entry : entries) {
            indexer.find(Path.of(entry)).ifPresentOrElse(
                    dir -> {
                        Logger.debug("restoring editor for '" + entry + "' -> found");
                        found.add(dir);
                    },
                    () -> Logger.debug("restoring editor for '" + entry + "' -> not indexed"));
        }

        return found;
    }

    private void openAll(final @NotNull Project p, final @NotNull List<DirectoryDto> found) {
        final @NotNull TestinEditors editors = Services.getInstance(p, TestinEditors.class);

        for (int i = 0; i < found.size(); i++) editors.open(p, found.get(i), i == found.size() - 1);
    }
}
