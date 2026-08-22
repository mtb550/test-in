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
}
