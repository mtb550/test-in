package org.testin.config;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * The rules that make {@code testin.yml} safe to hand to a tester (#6).
 * <p>
 * Two of them are the whole reason the feature is built this way. Reading must
 * never fail a startup, whatever the file turns out to contain - a repository
 * with a broken config opens unbound, not broken. And writing must never lose a
 * comment, because the comments are why the binding lives in a YAML file that
 * gets committed instead of in a settings dialog.
 * <p>
 * Both are one careless change away from breaking silently: a Jackson feature
 * flipped, or a writer that serializes the object instead of editing the line.
 */
public class TestinConfigTest {

    private static final String FULL = """
            # testin.yml
            version: 1
            testinProject: checkout-regression
            testinRepoUrl: https://github.com/acme/checkout-testcases
            defaultBranch: main
            """;

    @Test
    public void readsEveryKey() {
        final TestinProjectConfig config = TestinConfigLoader.parse(FULL, "full");

        assertEquals(config.version(), 1);
        assertEquals(config.testinProject(), "checkout-regression");
        assertEquals(config.testinRepoUrl(), "https://github.com/acme/checkout-testcases");
        assertEquals(config.defaultBranch(), "main");
        assertTrue(config.isBound());
        assertTrue(config.hasRepoUrl());
    }

    /**
     * A key the file leaves out is an empty value, never null. Every reader of a
     * config is unconditional, so a null here would surface as a crash somewhere
     * far from the file that caused it.
     */
    @Test
    public void absentKeysAreEmpty() {
        final TestinProjectConfig config = TestinConfigLoader.parse("version: 1\n", "partial");

        assertEquals(config.testinProject(), "");
        assertEquals(config.testinRepoUrl(), "");
        assertEquals(config.defaultBranch(), "");
        assertFalse(config.isBound());
        assertFalse(config.hasRepoUrl());
    }

    /**
     * A key nobody knows is ignored and the rest of the file still counts - a
     * config written by a later build must not cost this one its binding.
     */
    @Test
    public void unknownKeyDoesNotStopTheRest() {
        final TestinProjectConfig config = TestinConfigLoader.parse("""
                testinProject: checkout-regression
                somethingFromALaterBuild: true
                """, "unknown-key");

        assertEquals(config.testinProject(), "checkout-regression");
    }

    /**
     * Malformed and empty files both open unbound instead of throwing, because
     * this runs during startup and startup has to finish.
     */
    @Test
    public void brokenFilesOpenUnbound() {
        assertSame(TestinConfigLoader.parse("testinProject: [unclosed\n", "malformed"), TestinProjectConfig.EMPTY);
        assertSame(TestinConfigLoader.parse("   \n", "blank"), TestinProjectConfig.EMPTY);
    }

    /**
     * The URL reaches {@code git clone}, so a value carrying a shell
     * metacharacter is dropped when it is read, not when it is run.
     */
    @Test
    public void refusesARepoUrlThatIsNotOne() {
        assertEquals(TestinConfigLoader.parse("testinRepoUrl: \"https://x.com/r; rm -rf /\"\n", "injected").testinRepoUrl(), "");
        assertEquals(TestinConfigLoader.parse("testinRepoUrl: file:///etc/passwd\n", "scheme").testinRepoUrl(), "");
        assertEquals(TestinConfigLoader.parse("testinRepoUrl: git@github.com:acme/cases.git\n", "ssh").testinRepoUrl(),
                "git@github.com:acme/cases.git");
    }

    /**
     * The one that matters most: setting a key rewrites that key's line and
     * nothing else. Comments, blank lines, key order and indentation all survive,
     * which is what a serializing writer would have destroyed.
     */
    @Test
    public void writingAKeyKeepsEveryOtherLine() {
        final String before = """
                # testin.yml - do not delete this comment
                version: 1

                # the suite this repository drives
                testinProject: old-name
                defaultBranch: main
                """;

        assertEquals(TestinConfigWriter.apply(before, "testinProject", "new-name"), """
                # testin.yml - do not delete this comment
                version: 1

                # the suite this repository drives
                testinProject: new-name
                defaultBranch: main
                """);
    }

    /**
     * A key that is not in the file yet is appended, and the file that was there
     * is untouched above it.
     */
    @Test
    public void appendsAKeyThatIsNotThere() {
        assertEquals(TestinConfigWriter.apply("version: 1\n", "testinProject", "checkout"),
                "version: 1\ntestinProject: checkout\n");

        // No trailing newline before, one line added after, nothing lost between.
        assertEquals(TestinConfigWriter.apply("version: 1", "testinProject", "checkout"),
                "version: 1\ntestinProject: checkout\n");
    }

    /**
     * Windows line endings survive a write, including on the line that changed -
     * otherwise every binding shows up as a whole-file diff.
     */
    @Test
    public void keepsWindowsLineEndings() {
        assertEquals(TestinConfigWriter.apply("version: 1\r\ntestinProject: old\r\n", "testinProject", "new"),
                "version: 1\r\ntestinProject: new\r\n");
    }

    /**
     * A value that would change meaning as plain YAML is quoted, so what is read
     * back is what was written.
     */
    @Test
    public void quotesAValueThatNeedsIt() {
        assertEquals(TestinConfigWriter.apply("", "testinProject", "needs: quoting"), "testinProject: \"needs: quoting\"\n");
        assertEquals(TestinConfigWriter.apply("", "testinProject", ""), "testinProject: \"\"\n");
        assertEquals(TestinConfigWriter.apply("", "testinProject", "plain-name"), "testinProject: plain-name\n");
    }

    /**
     * A key named inside a comment is not the key. Replacing the commented line
     * would leave the real assignment below it and the file saying two things.
     */
    @Test
    public void aCommentedKeyIsNotTheKey() {
        assertEquals(TestinConfigWriter.apply("# testinProject: example\ntestinProject: old\n", "testinProject", "new"),
                "# testinProject: example\ntestinProject: new\n");
    }

    /**
     * The same name indented under a block is a different key. The format is flat
     * today, so this is about the blocks the file is expected to grow - a writer
     * that matched on the name alone would reach into one of them.
     */
    @Test
    public void aNestedKeyIsNotTheKey() {
        assertEquals(TestinConfigWriter.apply("report:\n  testinProject: inner\n", "testinProject", "outer"),
                "report:\n  testinProject: inner\ntestinProject: outer\n");
    }

    /**
     * What is written comes back the same way it is read - the round trip the
     * onboarding flow depends on.
     */
    @Test
    public void writtenValuesReadBack() {
        String content = TestinConfigWriter.apply("version: 1\n", "testinProject", "checkout-regression");
        content = TestinConfigWriter.apply(content, "testinRepoUrl", "https://github.com/acme/cases.git");

        final TestinProjectConfig config = TestinConfigLoader.parse(content, "round-trip");
        assertEquals(config.testinProject(), "checkout-regression");
        assertEquals(config.testinRepoUrl(), "https://github.com/acme/cases.git");
    }
}
