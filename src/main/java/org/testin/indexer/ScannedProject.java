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

/**
 * UC-INTERNAL-002, Rule-INTERNAL-021.
 * <p>
 * One pass of the scanner's reading, held on its own until the pass is finished.
 * <p>
 * The scan used to write straight into the index, and to empty the project out
 * of it first so that a rescan forgot what had disappeared. Between those two
 * the index held nothing about the project, and a rescan is a pull, a branch
 * switch, a hand edit or Refresh - all of which happen while a
 * tester is working. In that window {@code getTestRunByPath} and
 * {@code getTestSetDirByPath} answer for a node that is on disk and not in the
 * index, which is the one thing they are written to treat as a mistake in the
 * plugin: P or F or B on a row that was not executing raised an internal error,
 * a verdict on the executing row was dropped while the editor went on saying
 * Passed, and a test case saved then was stamped as created by whoever was
 * watching, because the index had no record of it existing (#312, A1).
 * <p>
 * So the pass reads into this instead, and {@link IndexerDataStore#swapIn} puts
 * it in when it is complete - adding before removing, so a node that is on disk
 * in both passes is never once absent. A pass that throws or is canceled is
 * simply not swapped in, and the index goes on holding what it held, which is a
 * better answer than half a project either way.
 * <p>
 * Concurrent maps because the case files of one test set are read in parallel.
 */
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

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-082.
     * <p>
     * Test case files this pass met whose identity another file had already
     * taken, named by the set they are in and the file itself.
     * <p>
     * A test case is identified by its file name, and the index holds one case
     * per identity - so the second file of a pair is read, put over the first,
     * and then reachable from neither set: the set that owns it lists the id and
     * gets the other set's case back, and the set that owned the first gets the
     * second's. An edit in one showed in the other and a removal hid its twin
     * (#312, A3).
     * <p>
     * Copying a test case file by hand is how a tester meets this, and nothing
     * said a word about it. The read cannot mend it - which of the two should
     * keep the identity is not the plugin's to decide - so it names them, in the
     * same notification the unread folders get.
     */
    private final @NotNull Set<String> clashingCases = ConcurrentHashMap.newKeySet();

    /**
     * The identities {@link #clashingCases} names, so the hand-named files
     * below can leave them out.
     */
    private final @NotNull Set<UUID> clashingIds = ConcurrentHashMap.newKeySet();

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-084.
     * <p>
     * Test case files whose name is not their id - written by hand - by the id
     * inside them.
     */
    private final @NotNull Map<UUID, Path> handNamedFiles = new ConcurrentHashMap<>();

    /**
     * UC-SHARE-002, Rule-SHARE-001.
     * <p>
     * The test case files this pass could not read, by the test set they are
     * in - what an export has to say is missing from it.
     */
    /**
     * Rule-INTERNAL-011.
     * <p>
     * The result files that would not parse, as {@code <run>/<file>}, for the one
     * notification the scan raises about them (#305, S21).
     */
    private final @NotNull Set<String> unreadableResults = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<String, Set<String>> unreadableCases = new ConcurrentHashMap<>();

    /**
     * UC-INTERNAL-004, Rule-INTERNAL-084.
     * <p>
     * The hand-named files that are the only file of their identity. One that
     * another file also claims is left out: which of the two is the case is the
     * tester's to decide, and a save must not take the other away.
     */
    @NotNull Map<UUID, Path> handNamedFilesAlone() {
        final @NotNull Map<UUID, Path> alone = new HashMap<>(handNamedFiles);
        alone.keySet().removeAll(clashingIds);

        return alone;
    }
}
