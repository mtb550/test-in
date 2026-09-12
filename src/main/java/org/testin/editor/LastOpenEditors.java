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

/**
 * Which Testin tabs were open when the project last closed, so opening it again
 * puts the tester back where they were.
 * <p>
 * Split out of {@code TestinEditors} (#291). That class finds, opens, closes and
 * reloads the editor showing a node - all of it about an editor that exists
 * now. Remembering a list of paths across two runs of the IDE is a different
 * job with its own store, and the class it used to belong to was still named in
 * the log lines here long after it was folded in: they said
 * "EditorStateService", which has not existed in this source for some time and
 * sent anyone grepping for it nowhere.
 * <p>
 * The key is persisted, so it is what it always was. What the IDE has written
 * under it survives an upgrade, and a tester who closes the project on this
 * version opens it on the next.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class LastOpenEditors {

    private static final @NotNull String OPEN_EDITORS_KEY = "testin.openEditors";

    /**
     * Rule-EDITOR-PANEL-015.
     * <p>
     * Writes the open tabs down, before {@code closeAll} makes there be none.
     * The order is the caller's - {@link SaveOnProjectClose} is the one thing
     * that knows the IDE is about to save its own tab list.
     */
    public void remember(final @NotNull Project p) {
        try {
            final @NotNull List<String> entries = pathsOfOpen(p);

            PropertiesComponent.getInstance(p).setValue(OPEN_EDITORS_KEY,
                    entries.isEmpty() ? null : String.join(";", entries));

        } catch (final Exception ex) {
            Logger.error("Failed to save open editors: " + ex.getMessage());
        }
    }

    /**
     * A path per open editor, and nothing else. The entry used to carry a "ts"
     * or "tr" prefix saying which kind of node it was, which the restore parsed
     * back to pick a lookup.
     * <p>
     * The indexer finds a node of any kind by path, and the open decides the
     * editor type from the node's own class. The prefix said nothing the path
     * did not.
     */
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

    /**
     * The nodes those paths still name. A remembered editor whose node is not
     * there any more is not reopened: the path was written last time the project
     * closed and the node may have been removed since.
     */
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

    /**
     * Only the last asks for the cursor. Every one of them used to, and each
     * open pumps the event queue while it waits, so the second request arrived
     * while the first was still focusing and the platform logged "Cannot focus
     * editor ... reason=selection changed". The last tab won either way; now it
     * wins without the race (#160).
     */
    private void openAll(final @NotNull Project p, final @NotNull List<DirectoryDto> found) {
        final @NotNull TestinEditors editors = Services.getInstance(p, TestinEditors.class);

        for (int i = 0; i < found.size(); i++) editors.open(p, found.get(i), i == found.size() - 1);
    }
}
