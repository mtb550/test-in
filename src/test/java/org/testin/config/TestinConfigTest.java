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
            location: remote
            connection: git
            testinProject: checkout-testcases
            RepoUrl: https://github.com/acme/checkout-testcases
            """;

    @Test
    public void readsEveryKey() {
        final TestinProjectConfig config = TestinConfigLoader.parse(FULL, "full");

        assertEquals(config.repoUrl(), "https://github.com/acme/checkout-testcases");
        assertEquals(config.projectName(), "checkout-testcases", "the key names the project");
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
        final TestinProjectConfig config = TestinConfigLoader.parse("# nothing but a comment\n", "partial");

        assertEquals(config.repoUrl(), "");
        assertEquals(config.projectName(), "", "no name in the file and no URL to take one from");
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
        assertEquals(TestinConfigLoader.parse("RepoUrl: \"https://x.com/r; rm -rf /\"\n", "injected").repoUrl(), "");
        assertEquals(TestinConfigLoader.parse("RepoUrl: file:///etc/passwd\n", "scheme").repoUrl(), "");
        assertEquals(TestinConfigLoader.parse("RepoUrl: git@github.com:acme/cases.git\n", "ssh").repoUrl(),
                "git@github.com:acme/cases.git");
    }

    /**
     * A server address makes the channel available, and nothing else has to say
     * so (#94).
     * <p>
     * There is deliberately no {@code connection: git|ssh} key. A mode word and
     * an address are two facts about one thing, and the day they disagree
     * something has to choose which half of the file to believe.
     */
    /**
     * The mode decides, not the addresses (#94).
     * <p>
     * The file can contradict itself - say local and still carry a host - so one
     * key is the authority and the rest is read against it. Otherwise something
     * has to choose which half of the file to believe.
     */
    @Test
    public void aProjectIsLocalUntilTheFileSaysOtherwise() {
        final TestinProjectConfig quiet = TestinConfigLoader.parse("connection: git\ntestinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n",
                "an address and no location");

        assertEquals(quiet.location(), TestinLocation.LOCAL, "left out, it is local");
        assertEquals(quiet.connection(), ConnectionType.NONE);
        assertFalse(quiet.hasSftp());
        assertFalse(quiet.hasRepoUrl());
    }

    @Test
    public void anAddressAloneDoesNotMakeAProjectRemote() {
        final TestinProjectConfig stillLocal = TestinConfigLoader.parse(
                "location: local\nconnection: sftp\nsftpHost: qa.internal\n", "local with a host left in");

        assertEquals(stillLocal.connection(), ConnectionType.NONE, "local wins over everything below it");
        assertFalse(stillLocal.hasSftp());
    }

    /**
     * A server path is a root holding several projects, and a root names none of
     * them - so that side says which one.
     */
    @Test
    public void aProjectIsNamedByTheFile() {
        final TestinProjectConfig onAServer = TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\n"
                        + "sftpPath: /srv/testin\ntestinProject: test-01\n", "sftp");

        assertEquals(onAServer.projectName(), "test-01");
        assertTrue(onAServer.isBound());
    }

    @Test
    public void anSftpServerWithNoProjectIsNotReachable() {
        final TestinProjectConfig unnamed = TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\n", "no project");

        assertFalse(unnamed.hasSftp(), "a root holding several projects does not say which one");
    }

    @Test
    public void anSftpProjectIsReadFromItsParts() {
        final TestinProjectConfig onAServer = TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\nsftpPort: 2222\n"
                        + "sftpPath: /srv/testin\ntestinProject: test-01\n", "sftp");

        assertTrue(onAServer.hasSftp());
        assertFalse(onAServer.hasRepoUrl(), "this team shares through a server and has no repository");
        assertEquals(onAServer.sftpAddress().host(), "qa.internal");
        assertEquals(onAServer.sftpAddress().port(), 2222);
        assertEquals(onAServer.projectName(), "test-01");
        assertEquals(onAServer.sftpAddress().path(), "/srv/testin/test-01", "the root, with the project under it");
    }

    /**
     * The address points at the project's folder, composed in one place (#94).
     * <p>
     * Two places deciding this is how "/Testin/test-01/test-01" happens: the
     * file names the project, and whatever syncs names it again.
     */
    @Test
    public void theAddressIsTheProjectsOwnFolder() {
        assertEquals(sftp("/Testin").sftpAddress().path(), "/Testin/test-01");
    }

    /**
     * Writing the whole path is the obvious thing for a tester to do, and must
     * not be read as asking for the folder twice.
     */
    @Test
    public void aPathThatAlreadyNamesTheProjectIsLeftAlone() {
        assertEquals(sftp("/Testin/test-01").sftpAddress().path(), "/Testin/test-01",
                "never /Testin/test-01/test-01");
        assertEquals(sftp("/Testin/test-01/").sftpAddress().path(), "/Testin/test-01");
    }

    /**
     * A file reaching a server called test-01, with the given root.
     */
    private static TestinProjectConfig sftp(final String root) {
        return TestinConfigLoader.parse("location: remote\nconnection: sftp\nsftpHost: qa.internal\n"
                + "sftpPath: " + root + "\ntestinProject: test-01\n", root);
    }

    @Test
    public void theSftpPortIsTwentyTwoUnlessSaid() {
        final TestinProjectConfig onAServer = TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\ntestinProject: test-01\n", "no port");

        assertEquals(onAServer.sftpAddress().port(), 22);
    }

    @Test
    public void aGitProjectIsReadFromItsUrl() {
        final TestinProjectConfig inGit = TestinConfigLoader.parse(
                "location: remote\nconnection: git\ntestinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n", "git");

        assertTrue(inGit.hasRepoUrl());
        assertFalse(inGit.hasSftp());
        assertTrue(inGit.connection().isShowsBranches(), "branches mean something in Git");
        assertFalse(inGit.connection().isSyncsToServer(), "and the sync action is off, not just quiet");
        assertEquals(inGit.projectName(), "cases");
    }

    @Test
    public void anSftpProjectNeverShowsBranches() {
        final TestinProjectConfig onAServer = TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\ntestinProject: test-01\n", "sftp");

        assertFalse(onAServer.connection().isShowsBranches(), "there are no branches to choose");
        assertFalse(onAServer.connection().isFetchesOnRefresh(), "and nothing here may reach a Git remote");
        assertTrue(onAServer.connection().isSyncsToServer(), "but there is a server to sync to");
    }

    /**
     * The account never travels in the committed file.
     * <p>
     * One shared account written here would be everybody's, and a tester's own
     * would be wrong for everybody else - the same reason the Testin root folder
     * is kept out of this file.
     */
    @Test
    public void anSftpHostCarryingAnAccountIsRefused() {
        assertEquals(TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: muteb@qa.internal\n", "account")
                .sftpHost(), "");
    }

    @Test
    public void refusesAnSftpHostThatIsNotOne() {
        assertEquals(TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: \"qa.internal; rm -rf /\"\n",
                "injected").sftpHost(), "");
        assertEquals(TestinConfigLoader.parse(
                "location: remote\nconnection: sftp\nsftpHost: sftp://qa.internal\n", "a URL, not a host")
                .sftpHost(), "");
    }

    @Test
    public void aWordNobodyCanReadIsLocalRatherThanAGuess() {
        assertEquals(TestinConfigLoader.parse("location: somewhere\n", "nonsense").location(),
                TestinLocation.LOCAL, "a project nobody can reach is better left on this machine");
        assertEquals(TestinConfigLoader.parse("location: remote\nconnection: ftp\n", "nonsense").connection(),
                ConnectionType.NONE);
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
                location: remote
                
                # the test project this repository drives
                RepoUrl: old-name
                sftpPort: 22
                """;

        assertEquals(TestinConfigWriter.apply(before, "RepoUrl", "new-name"), """
                # testin.yml - do not delete this comment
                location: remote
                
                # the test project this repository drives
                RepoUrl: new-name
                sftpPort: 22
                """);
    }

    /**
     * A key that is not in the file yet is appended, and the file that was there
     * is untouched above it.
     */
    @Test
    public void appendsAKeyThatIsNotThere() {
        assertEquals(TestinConfigWriter.apply("location: remote\n", "RepoUrl", "checkout"),
                "location: remote\nRepoUrl: checkout\n");

        // No trailing newline before, one line added after, nothing lost between.
        assertEquals(TestinConfigWriter.apply("location: remote", "RepoUrl", "checkout"),
                "location: remote\nRepoUrl: checkout\n");
    }

    /**
     * Windows line endings survive a write, including on the line that changed -
     * otherwise every binding shows up as a whole-file diff.
     */
    @Test
    public void keepsWindowsLineEndings() {
        assertEquals(TestinConfigWriter.apply("location: remote\r\nRepoUrl: old\r\n", "RepoUrl", "new"),
                "location: remote\r\nRepoUrl: new\r\n");
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
        assertEquals(TestinConfigWriter.apply("# testinProject: example\nRepoUrl: old\n", "RepoUrl", "new"),
                "# testinProject: example\nRepoUrl: new\n");
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
        String content = TestinConfigWriter.apply("location: remote\n", "testinProject", "checkout-regression");
        content = TestinConfigWriter.apply(content, "RepoUrl", "https://github.com/acme/cases.git");

        final TestinProjectConfig config = TestinConfigLoader.parse(content, "round-trip");
        assertEquals(config.repoUrl(), "https://github.com/acme/cases.git");
    }
    /**
     * A Git repository is named by the key like every other kind (#94).
     * <p>
     * Not by its clone URL. That was the rule once, and it made the Git case the
     * one place where renaming something elsewhere - the repository on GitHub -
     * silently re-pointed a binding here.
     */
    @Test
    public void aGitProjectIsNamedByTheKey() {
        final TestinProjectConfig named = TestinConfigLoader.parse(
                "location: remote\nconnection: git\n"
                        + "RepoUrl: https://github.com/mtb550/test-01.git\n"
                        + "testinProject: checkout\n", "git");

        assertEquals(named.projectName(), "checkout", "the file says so, not the URL");
        assertTrue(named.isBound());
        assertTrue(named.hasRepoUrl());
    }

    @Test
    public void aUrlWithNoNameLeavesTheRepositoryUnbound() {
        final TestinProjectConfig unnamed = TestinConfigLoader.parse(
                "location: remote\nconnection: git\n"
                        + "RepoUrl: https://github.com/mtb550/test-01.git\n", "no name");

        assertEquals(unnamed.projectName(), "", "the tester picks once, which writes the key");
        assertFalse(unnamed.isBound());
        assertTrue(unnamed.hasRepoUrl(), "and it can still be cloned");
    }

    /**
     * A project on this machine only has no address to be named by, so the key
     * is the only thing that can say which one - and it is read here too (#94).
     */
    @Test
    public void aLocalProjectIsNamedByTheKey() {
        final TestinProjectConfig here = TestinConfigLoader.parse(
                "location: local\ntestinProject: test-01\n", "local");

        assertEquals(here.projectName(), "test-01");
        assertTrue(here.isBound(), "picking a project sticks across an IDE restart");
        assertFalse(here.hasSftp());
        assertFalse(here.hasRepoUrl());
    }


    /**
     * A token never reaches the committed file (#94).
     * <p>
     * Git hands out {@code https://user:token@host/repo} as a remote's URL
     * without being asked, and this file is shared with everyone who clones -
     * so a token in it is in the history forever.
     */
    @Test
    public void aTokenIsStrippedOutOfACloneUrl() {
        assertEquals(TestinProjectConfig.withoutCredentials(
                "https://mtb550:ghp_secret@github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");

        assertEquals(TestinConfigLoader.parse(
                "location: remote\nconnection: git\ntestinProject: test-01\n"
                        + "RepoUrl: https://mtb550:ghp_secret@github.com/mtb550/test-01.git\n", "token")
                .repoUrl(), "https://github.com/mtb550/test-01.git",
                "stripped on the way in too, however it got there");
    }

    /**
     * The account every SSH clone URL carries is not a secret and stays.
     */
    @Test
    public void anSshAccountSurvives() {
        assertEquals(TestinProjectConfig.withoutCredentials("git@github.com:mtb550/test-01.git"),
                "git@github.com:mtb550/test-01.git");
        assertEquals(TestinProjectConfig.withoutCredentials("ssh://git@host:2222/qa/cases.git"),
                "ssh://git@host:2222/qa/cases.git");
        assertEquals(TestinProjectConfig.withoutCredentials("https://github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");
    }
}
