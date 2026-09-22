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

package org.testin.git;

import org.testin.model.DirectoryType;
import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A run's screenshots in Git (#313, Rule-SHARE-112): the review lists none of
 * them on its own, and committing a result carries the ones its run's folder
 * gained or lost - and no other run's.
 */
public class ScreenshotsInGitTest {

    private static final String RESULT = "runs/cycle38/4fd2a19b-59c7-44df-8cc4-ec5d293b18e9.ri";
    private static final String ADDED = "runs/cycle38/k3f9a.png";
    private static final String REMOVED = "runs/cycle38/q81zd.png";
    private static final String OTHER_RUN = "runs/cycle39/m4x0c.png";

    /**
     * Makes the folder a real test run, which is what the marker beside the
     * picture says.
     */
    private static void markARun(final Path root) {
        try {
            final Path at = root.resolve("runs/cycle38");
            Files.createDirectories(at);
            Files.writeString(at.resolve(DirectoryType.TR.getMarker()), "{}");
        } catch (final Exception e) {
            throw new AssertionError("could not write the run's marker", e);
        }
    }

    @Test
    public void theReviewListsNoScreenshotRow() {
        try {
            final Path root = Files.createTempDirectory("testin-git-screenshots");
            markARun(root);

            assertTrue(GitDiffProcessor.toDiffs(List.of(" D " + REMOVED), root, RealMapper.build(), path -> "", id -> Optional.empty()).isEmpty(),
                    "a screenshot arrives or goes with its run, so it has no row of its own");
        } catch (final Exception e) {
            throw new AssertionError("the review could not be built", e);
        }
    }

    /**
     * The other half of the rule, and the reason a marker decides it: a picture
     * is a screenshot because it sits in a run, not because of what it is called.
     * A five-character PNG a tester keeps beside a test set is a file like any
     * other, and hiding it left them unable to commit it at all (#305, S29).
     */
    @Test
    public void aPictureOutsideARunIsAFileLikeAnyOther() {
        try {
            final Path root = Files.createTempDirectory("testin-git-screenshots");
            final String beside = "cases/ts2/k3f9a.png";
            Files.createDirectories(root.resolve("cases/ts2"));
            Files.writeString(root.resolve(beside), "not really a picture");

            assertEquals(GitDiffProcessor.toDiffs(List.of("?? " + beside), root, RealMapper.build(), path -> "", id -> Optional.empty()).size(), 1,
                    "a PNG in a test set has no run to travel with, so the review has to list it");
        } catch (final Exception e) {
            throw new AssertionError("the review could not be built", e);
        }
    }

    @Test
    public void committingARunCarriesTheScreenshotsItsFolderGainedOrLost() {
        final List<String> status = List.of(" M " + RESULT, "?? " + ADDED, " D " + REMOVED, "?? " + OTHER_RUN);

        assertEquals(GitCommits.screenshotsAlongside(status, Set.of(RESULT)), Set.of(ADDED, REMOVED),
                "the screenshots a committed result named, or stopped naming, go with it");
    }

    @Test
    public void committingNoRunCarriesNoScreenshot() {
        assertTrue(GitCommits.screenshotsAlongside(List.of("?? " + ADDED), Set.of("cases/ts2/4fd2a19b.tc")).isEmpty());
    }
}
