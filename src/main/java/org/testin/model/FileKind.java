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

package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Rule-INTERNAL-011.
 * <p>
 * What a file in the test data is, and the name a record is written under: one
 * answer, where six places each had their own and five of them guessed - by the
 * folder above the file, by a uuid-shaped name, or by reading the file and
 * looking for a field (#305).
 * <p>
 * The name decides, and nothing else: a marker is one of the seven fixed names
 * {@link DirectoryType} owns, a test case ends in {@code .tc}, a run item in
 * {@code .ri}. Both are named by the id of what they hold, so the extension is
 * spelled here and the id read back here - the two directions of one fact
 * (#305, S24).
 * <p>
 * A screenshot is the one kind a name cannot settle on its own: five letters or
 * digits and {@code .png} is also an ordinary picture somebody put in a test set,
 * so it counts only inside a test run, which is what {@link #of(Path,
 * DirectoryType)} knows and {@link #of(Path)} does not (#305, S29).
 * <p>
 * Pure - a path in, a kind out, no disk read - so every package may ask it.
 */
@Getter
@AllArgsConstructor
public enum FileKind {

    /**
     * The dotfile that makes a folder a node, and holds that folder's own facts.
     * Its name is fixed per kind of folder, so it carries no extension here.
     */
    MARKER(""),

    TEST_CASE(".tc"),

    RUN_ITEM(".ri"),

    SCREENSHOT(".png"),

    /**
     * Anything else: a file a tester put there, and one Testin neither writes nor
     * reads. Listed, committed and left alone.
     */
    OTHER("");

    private final @NotNull String extension;

    /**
     * Rule-INTERNAL-011.
     * <p>
     * The kind of file this path is, by its name. Never {@link #SCREENSHOT}: a
     * five-character picture is one only inside a test run, and a path alone does
     * not say where it sits - {@link #of(Path, DirectoryType)} is the question
     * with that half in it.
     */
    public static @NotNull FileKind of(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        if (DirectoryType.byMarker(name).isPresent()) return MARKER;
        if (name.endsWith(TEST_CASE.extension)) return TEST_CASE;
        if (name.endsWith(RUN_ITEM.extension)) return RUN_ITEM;

        return OTHER;
    }

    /**
     * Rule-INTERNAL-011.
     * <p>
     * The same, for a caller that knows which kind of folder the file sits in -
     * the scan, the writers, the commit. Only there can a picture be a
     * screenshot, which is why a {@code logo1.png} a tester keeps beside a test
     * set is {@link #OTHER} and stays visible (#305, S29).
     */
    public static @NotNull FileKind of(final @NotNull Path file, final @NotNull DirectoryType folder) {
        final @NotNull FileKind byName = of(file);
        if (byName != OTHER) return byName;

        return folder == DirectoryType.TR && TestRunDirectoryDto.isScreenshot(file) ? SCREENSHOT : OTHER;
    }

    /**
     * Rule-INTERNAL-012.
     * <p>
     * What a record with this id is called. The file name is the record's
     * identity, so this is the only place that spells it - a second speller is
     * how a copied test set came to be written as files nothing reads (#305,
     * S24).
     */
    public @NotNull String fileName(final @NotNull UUID id) {
        return id + extension;
    }

    /**
     * Rule-INTERNAL-012.
     * <p>
     * The id this file name carries, and empty when the name is not one Testin
     * wrote - a file a tester named by hand keeps the id inside it
     * (Rule-INTERNAL-084).
     */
    public @NotNull Optional<UUID> idIn(final @NotNull Path file) {
        final @NotNull String name = String.valueOf(file.getFileName());
        if (!name.endsWith(extension) || extension.isEmpty()) return Optional.empty();

        try {
            return Optional.of(UUID.fromString(name.substring(0, name.length() - extension.length())));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }
}
