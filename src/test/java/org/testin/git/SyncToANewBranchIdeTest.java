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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testin.git.LocalGit.mustGit;

public class SyncToANewBranchIdeTest extends BasePlatformTestCase {
    private static final @NotNull String BRANCH = "feature/new-cases";

    private Path base;
    private Path remote;
    private Path work;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertTrue("Git is not on the PATH, so a sync cannot be exercised", LocalGit.onThePath());

        base = Files.createTempDirectory("testin-sync");
        remote = Files.createDirectories(base.resolve("remote.git"));
        work = Files.createDirectories(base.resolve("work"));

        mustGit(remote, "init", "--bare", "--initial-branch=main");
        mustGit(work, "init", "--initial-branch=main");
        mustGit(work, "config", "user.name", "Testin Test");
        mustGit(work, "config", "user.email", "testin@example.invalid");
        mustGit(work, "remote", "add", "origin", remoteUrl());

        Files.writeString(work.resolve("first.tc"), "{}");
        mustGit(work, "add", "first.tc");
        mustGit(work, "commit", "-m", "first");
        mustGit(work, "push", "-u", "origin", "main");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(base);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull String remoteUrl() {
        return remote.toUri().toString();
    }

    // UC-SHARE-016, Rule-SHARE-069, Rule-SHARE-120
    public void testSyncPushesABranchTheRemoteDoesNotHaveYet() {
        mustGit(work, "checkout", "-b", BRANCH);
        try {
            Files.writeString(work.resolve("second.tc"), "{}");
        } catch (final IOException ex) {
            throw new AssertionError("Could not write the test case: " + ex.getMessage(), ex);
        }
        mustGit(work, "add", "second.tc");
        mustGit(work, "commit", "-m", "second");

        final @NotNull GitCommits commits = new GitCommits(getProject());
        commits.pullWhereTheRemoteHasBranch(work, "origin", remoteUrl(), BRANCH);
        commits.push(work, "origin", remoteUrl(), BRANCH);

        assertEquals(mustGit(work, "rev-parse", "HEAD").trim(), mustGit(remote, "rev-parse", BRANCH).trim());
    }
}
