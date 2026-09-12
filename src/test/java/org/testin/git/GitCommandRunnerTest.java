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

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * How a path list reaches Git (#89).
 * <p>
 * Windows refuses to start a process whose command line exceeds 32,767
 * characters, so the paths of a commit travel in a file. What that file holds
 * is checked here, without starting Git: the end-to-end proof that Git reads it
 * back correctly is in {@code GitWorkflowTest}, which needs Git on the machine
 * and is skipped when there is none. This is not.
 */
public class GitCommandRunnerTest {

    @Test
    public void entriesAreSeparatedByTheOneByteAPathCannotContain() {
        final String written = new String(
                GitCommandRunner.pathspecBytes(List.of("test-01/Test Cases/Login/a.json", "test-01/.tp")),
                StandardCharsets.UTF_8);

        assertEquals(written, "test-01/Test Cases/Login/a.json\0test-01/.tp",
                "a space is an ordinary character in a test set name, so the separator cannot be one");
    }

    @Test
    public void nothingFollowsTheLastEntry() {
        final byte[] written = GitCommandRunner.pathspecBytes(List.of("test-01/.tp"));

        assertTrue(written.length > 0);
        assertEquals(written[written.length - 1], (byte) 'p',
                "a trailing separator leaves an empty pathspec behind it, and Git rejects the whole command over one");
    }

    @Test
    public void onePathIsJustThatPath() {
        assertEquals(new String(GitCommandRunner.pathspecBytes(List.of("a.json")), StandardCharsets.UTF_8), "a.json");
    }

    @Test
    public void aTokenInARemoteUrlNeverReachesTheLogOrTheTester() {
        assertEquals(GitSafeText.withoutCredentials(
                        "fatal: could not read from https://ghp_secret@github.com/owner/repo.git"),
                "fatal: could not read from https://***@github.com/owner/repo.git",
                "a token used as the username is the common form, and it has no colon to find it by");

        assertEquals(GitSafeText.withoutCredentials(
                        "remote: https://user:p%40ss@example.com/x rejected"),
                "remote: https://***@example.com/x rejected",
                "an escaped at-sign in the password is not the one that introduces the host");
    }

    @Test
    public void aUrlWithNothingToHideIsLeftAsItIs() {
        final String said = "fatal: repository 'https://github.com/owner/repo.git' not found";

        assertEquals(GitSafeText.withoutCredentials(said), said,
                "redacting what carries no credentials would make every ordinary failure unreadable");
    }
}
