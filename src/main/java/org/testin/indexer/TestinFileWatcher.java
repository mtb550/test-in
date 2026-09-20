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

package org.testin.indexer;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TestinFileWatcher implements AsyncFileListener {
    // UC-INTERNAL-003, Rule-INTERNAL-016
    @Override
    public @Nullable ChangeApplier prepareChange(final @NotNull List<? extends VFileEvent> events) {
        final @NotNull Set<Path> testProjects = changedTestProjects(events);
        if (testProjects.isEmpty()) return null;

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                Services.getInstance(Rescan.class).of(testProjects);
            }
        };
    }

    // UC-INTERNAL-003, Rule-INTERNAL-016, Rule-INTERNAL-019
    private static @NotNull Set<Path> changedTestProjects(final @NotNull List<? extends VFileEvent> events) {
        final @NotNull List<Path> roots = Arrays.stream(ProjectManager.getInstance().getOpenProjects())
                .filter(p -> !p.isDisposed())
                .map(p -> Services.getInstance(p, TestinRoot.class).absolutePath())
                .filter(TestinRoot::isConfigured)
                .distinct()
                .toList();
        final @NotNull OwnWrites ours = Services.getInstance(OwnWrites.class);
        final @NotNull Set<Path> testProjects = new HashSet<>();

        for (final VFileEvent event : events) {
            changedFile(event)
                    .filter(file -> !ours.areOurs(file))
                    .flatMap(file -> roots.stream().flatMap(root -> WatchedPath.testProjectOf(file, root).stream()).findFirst())
                    .ifPresent(testProjects::add);
        }

        return testProjects;
    }

    private static @NotNull Optional<Path> changedFile(final @NotNull VFileEvent event) {
        try {
            return Optional.of(Path.of(event.getPath()));
        } catch (final RuntimeException notAFileSystemPath) {
            return Optional.empty();
        }
    }
}
