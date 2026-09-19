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

    /**
     * {@code bugRepoUrl} is read like every other key, and the token a copied
     * address can carry is gone before anything holds it (#28).
     */
    @Test
    public void aBugRepositoryIsReadFromItsOwnKey() {
        final TestinProjectConfig config = TestinYml.parse(
                "testinProject: cases\nbugRepoUrl: https://mtb550:ghp_secret@github.com/mtb550/product.git\n", "bug repo");

        assertEquals(config.bugRepoUrl(), "https://github.com/mtb550/product.git", "a token never survives, as for RepoUrl");
        assertEquals(config.bugRepository().map(BugRepository::ghRepo).orElse(""), "github.com/mtb550/product");
    }

    /**
     * Left out, or naming no repository, there is nothing to file in - but what
     * the tester wrote stays, so the reason can say what is wrong with it.
     */
    @Test
    public void aBugRepoUrlThatNamesNoRepositoryIsKeptAndNotUsed() {
        assertTrue(TestinYml.parse("testinProject: cases\n", "none").bugRepository().isEmpty());
        assertEquals(TestinProjectConfig.EMPTY.bugRepoUrl(), "");

        final TestinProjectConfig wrong = TestinYml.parse(
                "bugRepoUrl: https://github.com/mtb550/product/issues\n", "issues page");
        assertEquals(wrong.bugRepoUrl(), "https://github.com/mtb550/product/issues");
        assertTrue(wrong.bugRepository().isEmpty());
    }

    @Test
    public void readsEveryKey() {
        final TestinProjectConfig config = TestinYml.parse(FULL, "full");

        assertEquals(config.repoUrl(), "https://github.com/acme/checkout-testcases");
        assertEquals(config.projectName(), "checkout-testcases", "the key names the project");
        assertTrue(config.hasRepoUrl());
    }

    /**
     * A key the file leaves out is an empty value, never null. Every reader of a
     * config is unconditional, so a null here would surface as a crash somewhere
     * far from the file that caused it.
     */
    @Test
    public void absentKeysAreEmpty() {
        final TestinProjectConfig config = TestinYml.parse("# nothing but a comment\n", "partial");

        assertEquals(config.repoUrl(), "");
        assertEquals(config.projectName(), "", "no name in the file and no URL to take one from");
        assertFalse(config.hasRepoUrl());
    }

    /**
     * A key nobody knows is ignored and the rest of the file still counts - a
     * config written by a later build must not cost this one its binding.
     */
    @Test
    public void unknownKeyDoesNotStopTheRest() {
        final TestinProjectConfig config = TestinYml.parse("""
                testinProject: checkout-regression
                somethingFromALaterBuild: true
                """, "unknown-key");

        // The whole point of the test, and it was not being made: the parse
        // reached the key it knew despite the one it did not. Without this the
        // method could not fail, whatever the loader did with the unknown key.
        assertEquals(config.projectName(), "checkout-regression",
                "a key from a later build must not cost this one its binding");
    }

    /**
     * Malformed and empty files both open unbound instead of throwing, because
     * this runs during startup and startup has to finish.
     */
    @Test
    public void brokenFilesOpenUnbound() {
        assertSame(TestinYml.parse("testinProject: [unclosed\n", "malformed"), TestinProjectConfig.UNREADABLE);
        assertSame(TestinYml.parse("   \n", "blank"), TestinProjectConfig.EMPTY);
    }


    /**
     * UC-TREE-PANEL-001.
     * <p>
     * A file that would not parse says exactly what an absent one says, and is
     * not the same state.
     * <p>
     * Both answer every question with nothing, which is why they were one value
     * for so long. What they are not is the same thing to tell a tester: nobody
     * has bound this repository yet is ordinary, and your file has a mistake in
     * it is a line to correct - and until this was told apart, a mistyped indent
     * was reported as the first and then bound over, writing a testinProject
     * line into the file on every open (#66, finding 10).
     */
    @Test
    public void aBrokenFileIsTheSameValueAndNotTheSameState() {
        final TestinProjectConfig broken = TestinYml.parse("testinProject: [unclosed\n", "malformed");
        final TestinProjectConfig absent = TestinYml.parse("   \n", "blank");

        assertEquals(broken, absent, "a file that would not parse has told us no more than one that is not there");

        assertTrue(broken.isUnreadable(), "the broken file is the one state a caller can act on differently");
        assertFalse(absent.isUnreadable(), "an absent file is not a broken one, and must not be reported as one");
        assertFalse(TestinProjectConfig.EMPTY.isUnreadable(), "saying nothing is not the same as failing to be read");
    }

    /**
     * The URL reaches {@code git clone}, so a value carrying a shell
     * metacharacter is dropped when it is read, not when it is run.
     */
    @Test
    public void refusesARepoUrlThatIsNotOne() {
        assertEquals(TestinYml.parse("RepoUrl: \"https://x.com/r; rm -rf /\"\n", "injected").repoUrl(), "");
        assertEquals(TestinYml.parse("RepoUrl: file:///etc/passwd\n", "scheme").repoUrl(), "");
        assertEquals(TestinYml.parse("RepoUrl: git@github.com:acme/cases.git\n", "ssh").repoUrl(),
                "git@github.com:acme/cases.git");
    }

    /**
     * The mode decides, not the addresses (#94).
     * <p>
     * The file can contradict itself - say local and still carry a host - so one
     * key is the authority and the rest is read against it. Otherwise something
     * has to choose which half of the file to believe.
     */
    @Test
    public void aProjectIsLocalUntilTheFileSaysOtherwise() {
        final TestinProjectConfig quiet = TestinYml.parse("connection: git\ntestinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n",
                "an address and no location");

        assertEquals(quiet.location(), TestinLocation.LOCAL, "left out, it is local");
        assertEquals(quiet.connection(), ConnectionType.NONE);
        assertFalse(quiet.hasSftp());
        assertFalse(quiet.hasRepoUrl());
    }

    @Test
    public void anAddressAloneDoesNotMakeAProjectRemote() {
        final TestinProjectConfig stillLocal = TestinYml.parse(
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
        final TestinProjectConfig onAServer = TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\n"
                        + "sftpPath: /srv/testin\ntestinProject: test-01\n", "sftp");

        assertEquals(onAServer.projectName(), "test-01");
    }

    /**
     * Which project is synced is the one selected in the tree, so the file needs
     * no name for a server to be reachable (#301).
     */
    @Test
    public void anSftpServerNeedsNoProjectInTheFile() {
        final TestinProjectConfig unnamed = TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\nsftpPath: /srv/testin\n", "no project");

        assertTrue(unnamed.missingForSftp().isEmpty());
        assertEquals(unnamed.sftpAddress("checkout").path(), "/srv/testin/checkout");
    }

    @Test
    public void anSftpProjectIsReadFromItsParts() {
        final TestinProjectConfig onAServer = TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\nsftpPort: 2222\n"
                        + "sftpPath: /srv/testin\ntestinProject: test-01\n", "sftp");

        assertTrue(onAServer.hasSftp());
        assertFalse(onAServer.hasRepoUrl(), "this team shares through a server and has no repository");
        assertEquals(onAServer.sftpAddress("test-01").host(), "qa.internal");
        assertEquals(onAServer.sftpAddress("test-01").port(), 2222);
        assertEquals(onAServer.projectName(), "test-01");
        assertEquals(onAServer.sftpAddress("test-01").path(), "/srv/testin/test-01", "the root, with the project under it");
    }

    /**
     * The address points at the project's folder, composed in one place (#94).
     * <p>
     * Two places deciding this is how "/Testin/test-01/test-01" happens: the
     * file names the project, and whatever syncs names it again.
     */
    @Test
    public void theAddressIsTheProjectsOwnFolder() {
        assertEquals(sftp("/Testin").sftpAddress("test-01").path(), "/Testin/test-01");
    }

    /**
     * Writing the whole path is the obvious thing for a tester to do, and must
     * not be read as asking for the folder twice.
     */
    @Test
    public void aPathThatAlreadyNamesTheProjectIsLeftAlone() {
        assertEquals(sftp("/Testin/test-01").sftpAddress("test-01").path(), "/Testin/test-01",
                "never /Testin/test-01/test-01");
        assertEquals(sftp("/Testin/test-01/").sftpAddress("test-01").path(), "/Testin/test-01");
    }

    /**
     * A file reaching a server called test-01, with the given root.
     */
    private static TestinProjectConfig sftp(final String root) {
        return TestinYml.parse("location: remote\nconnection: sftp\nsftpHost: qa.internal\n"
                + "sftpPath: " + root + "\ntestinProject: test-01\n", root);
    }

    @Test
    public void theSftpPortIsTwentyTwoUnlessSaid() {
        final TestinProjectConfig onAServer = TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\ntestinProject: test-01\n", "no port");

        assertEquals(onAServer.sftpAddress("test-01").port(), 22);
    }

    @Test
    public void aGitProjectIsReadFromItsUrl() {
        final TestinProjectConfig inGit = TestinYml.parse(
                "location: remote\nconnection: git\ntestinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n", "git");

        assertTrue(inGit.hasRepoUrl());
        assertFalse(inGit.hasSftp());
        assertFalse(inGit.connection().isSyncsToServer(), "and the sync action is off, not just quiet");
        assertEquals(inGit.projectName(), "cases");
    }

    /**
     * A server is the one thing that takes the branch box away: the folder
     * decides the rest (Rule-TREE-PANEL-108).
     */
    @Test
    public void anSftpProjectSyncsToAServer() {
        final TestinProjectConfig onAServer = TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: qa.internal\ntestinProject: test-01\n", "sftp");

        assertTrue(onAServer.connection().isSyncsToServer(),
                "there is a server to sync to, so there are no branches to choose and nothing may reach a Git remote");
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
        assertEquals(TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: muteb@qa.internal\n", "account")
                .sftpHost(), "");
    }

    @Test
    public void refusesAnSftpHostThatIsNotOne() {
        assertEquals(TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: \"qa.internal; rm -rf /\"\n",
                "injected").sftpHost(), "");
        assertEquals(TestinYml.parse(
                "location: remote\nconnection: sftp\nsftpHost: sftp://qa.internal\n", "a URL, not a host")
                .sftpHost(), "");
    }

    @Test
    public void aWordNobodyCanReadIsLocalRatherThanAGuess() {
        assertEquals(TestinYml.parse("location: somewhere\n", "nonsense").location(),
                TestinLocation.LOCAL, "a project nobody can reach is better left on this machine");
        assertEquals(TestinYml.parse("location: remote\nconnection: ftp\n", "nonsense").connection(),
                ConnectionType.NONE);
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
        final TestinProjectConfig named = TestinYml.parse(
                "location: remote\nconnection: git\n"
                        + "RepoUrl: https://github.com/mtb550/test-01.git\n"
                        + "testinProject: checkout\n", "git");

        assertEquals(named.projectName(), "checkout", "the file says so, not the URL");
        assertTrue(named.hasRepoUrl());
    }

    @Test
    public void aUrlWithNoNameLeavesTheRepositoryUnbound() {
        final TestinProjectConfig unnamed = TestinYml.parse(
                "location: remote\nconnection: git\n"
                        + "RepoUrl: https://github.com/mtb550/test-01.git\n", "no name");

        assertEquals(unnamed.projectName(), "", "the tester picks once, on this machine");
        assertTrue(unnamed.hasRepoUrl(), "and it can still be cloned");
    }

    /**
     * A project on this machine only has no address to be named by, so the key
     * is the only thing that can say which one - and it is read here too (#94).
     */
    @Test
    public void aLocalProjectIsNamedByTheKey() {
        final TestinProjectConfig here = TestinYml.parse(
                "location: local\ntestinProject: test-01\n", "local");

        assertEquals(here.projectName(), "test-01");
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

        assertEquals(TestinYml.parse(
                "location: remote\nconnection: git\ntestinProject: test-01\n"
                        + "RepoUrl: https://mtb550:ghp_secret@github.com/mtb550/test-01.git\n", "token")
                .repoUrl(), "https://github.com/mtb550/test-01.git",
                "stripped on the way in too, however it got there");
    }

    /**
     * The account every SSH clone URL carries is not a secret and stays.
     */
    /**
     * Rule-SHARE-004. The form GitHub documents for cloning with a token: the
     * token is the whole user part, with no colon. It passed through into the
     * committed file (#66, finding 164).
     */
    @Test
    public void aTokenWithNoColonIsStrippedOutOfAnHttpsUrl() {
        assertEquals(TestinProjectConfig.withoutCredentials("https://ghp_secret@github.com/mtb550/test-01.git"),
                "https://github.com/mtb550/test-01.git");
        assertEquals(TestinProjectConfig.withoutCredentials("HTTP://ghp_secret@intranet/qa/cases.git"),
                "HTTP://intranet/qa/cases.git");
    }

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
