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

import org.testin.util.RealMapper;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A run's screenshots in Git (#313, Rule-SHARE-112): the review lists none of
 * them on its own, and committing a run carries the ones its folder gained or
 * lost - and no other run's.
 */
public class ScreenshotsInGitTest {

    private static final String RUN = "runs/cycle38/run.json";
    private static final String ADDED = "runs/cycle38/0123456789abcdef.png";
    private static final String REMOVED = "runs/cycle38/1111111111111111.png";
    private static final String OTHER_RUN = "runs/cycle39/fedcba9876543210.png";

    @Test
    public void theReviewListsNoScreenshotRow() {
        try {
            final Path root = Files.createTempDirectory("testin-git-screenshots");

            assertTrue(GitDiffProcessor.toDiffs(List.of(" D " + REMOVED), root, RealMapper.build(), path -> "").isEmpty(),
                    "a screenshot arrives or goes with its run, so it has no row of its own");
        } catch (final Exception e) {
            throw new AssertionError("the review could not be built", e);
        }
    }

    @Test
    public void committingARunCarriesTheScreenshotsItsFolderGainedOrLost() {
        final List<String> status = List.of(" M " + RUN, "?? " + ADDED, " D " + REMOVED, "?? " + OTHER_RUN);

        assertEquals(GitCommits.screenshotsAlongside(status, Set.of(RUN)), Set.of(ADDED, REMOVED));
    }

    @Test
    public void committingNoRunCarriesNoScreenshot() {
        assertTrue(GitCommits.screenshotsAlongside(List.of("?? " + ADDED), Set.of("cases/ts2/4fd2a19b.json")).isEmpty());
    }
}
