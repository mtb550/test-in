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

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.services.Services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * What a sync carries in each direction: every file of a test project on the
 * way out, and what a server sent on the way back.
 * <p>
 * Files rather than nodes, deliberately. A sync exchanges files, and the node
 * they add up to comes back from the scan afterwards - going node by node would
 * mean the sync knowing which file is a case, a set or a run, which is the
 * question the scan exists to answer.
 * <p>
 * Package-private, and reached only through {@link ProjectIndexer}: reading and
 * writing test data has one door, and the transfer packages are not exempt from
 * it the way {@code git} is - Git writes the working tree itself, and a
 * transfer has no such claim.
 */
@AllArgsConstructor
final class SyncFiles {

    private final @NotNull Project p;

    /**
     * Every file in a test project, by the path a server names it with (#94).
     * <p>
     * Walked from disk rather than read out of the cache, and that is not a
     * detail: two directories in the sandbox project carry no marker, so the
     * scan skips them - and the four test cases inside them have never been in
     * the cache. A sync built on {@code getAllTestCases} would not upload those
     * four, and would then delete them from the server as files that no longer
     * exist.
     */
    @NotNull Map<String, byte[]> under(final @NotNull Path projectPath) {
        final @NotNull Map<String, byte[]> files = new TreeMap<>();

        try (Stream<Path> paths = Files.walk(projectPath)) {
            for (final Path file : paths.filter(Files::isRegularFile).toList()) {
                final @NotNull String relative =
                        projectPath.relativize(file).toString().replace(File.separatorChar, '/');

                if (ProjectIndexer.isGitsOwn(relative)) continue;

                files.put(relative, Files.readAllBytes(file));
            }
        } catch (final IOException ex) {
            Logger.error("Could not read the project at " + projectPath + ": " + ex.getMessage());
        }

        return files;
    }

    /**
     * Writes what arrived from a server into the project.
     * <p>
     * Through {@link TestDataFiles}, which refuses to write an empty file -
     * exactly the protection a transfer that was cut off halfway needs, because
     * an empty test case would be indexed as a case with no fields rather than
     * as a failure.
     */
    void accept(final @NotNull Path projectPath, final @NotNull Map<String, byte[]> files) {
        final @NotNull TestDataFiles writer = Services.getInstance(p, TestDataFiles.class);

        files.forEach((relative, content) -> writer.write(p, projectPath.resolve(relative), content));
        Logger.info("Wrote " + files.size() + " incoming files into " + projectPath);
    }

    /**
     * Removes the files the server no longer holds, once the tester has agreed
     * to it.
     */
    void remove(final @NotNull Path projectPath, final @NotNull Collection<String> relatives) {
        final @NotNull TestDataFiles files = Services.getInstance(p, TestDataFiles.class);

        relatives.forEach(relative -> files.delete(p, projectPath.resolve(relative), projectPath));
        Logger.info("Removed " + relatives.size() + " files the server no longer holds from " + projectPath);
    }
}
