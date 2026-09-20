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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// UC-INTERNAL-002, Rule-INTERNAL-021
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
final class ScannedProject {
    private final @NotNull Map<String, TestProjectDirectoryDto> projects = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestCasesMainDirectoryDto> testCasesMainDirs = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunsMainDirectoryDto> testRunsMainDirs = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestSetPackageDirectoryDto> testSetPackages = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunPackageDirectoryDto> testRunPackages = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestSetDirectoryDto> testSets = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunDirectoryDto> testRunDirs = new ConcurrentHashMap<>();
    private final @NotNull Map<String, TestRunDto> testRuns = new ConcurrentHashMap<>();
    private final @NotNull Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();
    private final @NotNull Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();

    // UC-INTERNAL-002, Rule-INTERNAL-082
    private final @NotNull Set<String> clashingCases = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<UUID> clashingIds = ConcurrentHashMap.newKeySet();

    // UC-INTERNAL-004, Rule-INTERNAL-084
    private final @NotNull Map<UUID, Path> handNamedFiles = new ConcurrentHashMap<>();

    // Rule-INTERNAL-011
    private final @NotNull Set<String> unreadableResults = ConcurrentHashMap.newKeySet();

    // UC-SHARE-002, Rule-SHARE-001
    private final @NotNull Map<String, Set<String>> unreadableCases = new ConcurrentHashMap<>();

    // UC-INTERNAL-004, Rule-INTERNAL-084
    @NotNull Map<UUID, Path> handNamedFilesAlone() {
        final @NotNull Map<UUID, Path> alone = new HashMap<>(handNamedFiles);
        alone.keySet().removeAll(clashingIds);

        return alone;
    }
}
