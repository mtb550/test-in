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

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.FileKind;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestDataFiles {
    // UC-INTERNAL-004, Rule-INTERNAL-033
    <T> boolean alreadyHolds(final @NotNull Project p, final @NotNull Path path, final @NotNull T content) {
        try {
            return Arrays.equals(Files.readAllBytes(path), Services.getInstance(p, Mapper.class).writeValueAsBytes(content));
        } catch (final IOException absentOrUnreadable) {
            return false;
        }
    }

    boolean alreadyHolds(final @NotNull Path path, final byte @NotNull [] bytes) {
        try {
            return Arrays.equals(Files.readAllBytes(path), bytes);
        } catch (final IOException absentOrUnreadable) {
            return false;
        }
    }

    <T> boolean write(final @NotNull Project p, final @NotNull Path path, final @NotNull T content) {
        return writeBytes(p, path, Services.getInstance(p, Mapper.class).writeValueAsBytes(content));
    }

    void write(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        writeBytes(p, path, jsonBytes);
    }

    byte @NotNull [] readBytes(final @NotNull Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException missingOrUnreadable) {
            Logger.warn("Could not read " + path + ": " + missingOrUnreadable.getMessage());
            return new byte[0];
        }
    }

    // Rule-INTERNAL-011
    @NotNull List<Path> resultsIn(final @NotNull Path runPath) {
        try (Stream<Path> inside = Files.list(runPath)) {
            return inside.filter(file -> FileKind.of(file) == FileKind.RUN_ITEM).toList();
        } catch (final IOException ex) {
            Logger.warn("Could not list the results in " + runPath + ": " + ex.getMessage());
            return List.of();
        }
    }

    @NotNull List<Path> screenshotsIn(final @NotNull Path runPath) {
        try (Stream<Path> inside = Files.list(runPath)) {
            return inside.filter(file -> FileKind.of(file, DirectoryType.TR) == FileKind.SCREENSHOT).toList();
        } catch (final IOException ex) {
            Logger.warn("Could not list the screenshots in " + runPath + ": " + ex.getMessage());
            return List.of();
        }
    }

    // UC-INTERNAL-003, Rule-INTERNAL-019
    private boolean writeBytes(final @NotNull Project p, final @NotNull Path path, final byte @NotNull [] jsonBytes) {
        if (jsonBytes.length == 0) {
            Logger.error("Refusing to write an empty file, which would erase it: " + path);
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("files.nothing.written", path.getFileName()));
            return false;
        }

        try {
            Services.getInstance(OwnWrites.class).record(p, path);

            FileUtil.createParentDirs(path.toFile());
            Files.write(path, jsonBytes);

            Services.getInstance(OwnWrites.class).wrote(p, path, jsonBytes);
            return true;
        } catch (final IOException ex) {
            reportWriteFailure(p, path, ex);
            return false;
        }
    }

    // Rule-INTERNAL-091
    boolean move(final @NotNull Project p, final @NotNull Path from, final @NotNull Path to) {
        try {
            Services.getInstance(OwnWrites.class).record(p, from);
            Services.getInstance(OwnWrites.class).record(p, to);

            Files.move(from, to);
            return true;
        } catch (final IOException ex) {
            Logger.error("Could not move " + from + " to " + to + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("files.unable.to.remove", ex.getMessage()));
            return false;
        }
    }

    // Rule-INTERNAL-091, Rule-INTERNAL-036
    boolean removeTree(final @NotNull Project p, final @NotNull Path folder) {
        try (Stream<Path> inside = Files.walk(folder)) {
            inside.forEach(each -> Services.getInstance(OwnWrites.class).record(p, each));
        } catch (final IOException ex) {
            Logger.warn("Could not claim what is inside " + folder + ": " + ex.getMessage());
        }

        if (Trash.accepted(p, folder)) return true;

        try (Stream<Path> inside = Files.walk(folder)) {
            for (final Path path : inside.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (final IOException ex) {
            Logger.error("Could not remove " + folder + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("files.unable.to.remove", ex.getMessage()));
            return false;
        }
    }

    // UC-INTERNAL-005, Rule-INTERNAL-036
    boolean delete(final @NotNull Project p, final @NotNull Path path) {
        try {
            Services.getInstance(OwnWrites.class).record(p, path);

            if (!Trash.accepted(p, path)) Files.deleteIfExists(path);
        } catch (final IOException ex) {
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("files.unable.to.remove", ex.getMessage()));
            Logger.error("unable to remove " + path + ": " + ex.getMessage());
            return false;
        }

        return true;
    }

    private void reportWriteFailure(final @NotNull Project p, final @NotNull Path path, final @NotNull IOException ex) {
        Services.getInstance(p, Notifier.class).error(p, Bundle.message("files.unable.to.write", ex.getMessage()));
        Logger.error("unable to write content: " + ex.getMessage());
        Logger.error("path" + path);
    }
}
