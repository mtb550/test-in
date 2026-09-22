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

import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * The rules that make {@code testin.yml} safe to hand to a tester (#6).
 * <p>
 * Two of them are the whole reason the feature is built this way. Reading must
 * never fail a startup, whatever the file turns out to contain - a repository
 * with a broken config opens unbound, not broken. And writing must never lose a
 * comment, because the comments are why the team's config lives in a YAML file
 * that gets committed instead of in a settings dialog.
 * <p>
 * Both are one careless change away from breaking silently: a Jackson feature
 * flipped, or a writer that serializes the object instead of editing the line.
 */
public class TestinConfigTest {

    private static final String FULL = """
            # testin.yml
            location: remote
            testinProject: checkout-testcases
            RepoUrl: https://github.com/acme/checkout-testcases
            """;

    /**
     * The lines Save to testin.yml writes for a test project cloned from Git -
     * built by the class that reads them, so a key spelled one way by the
     * writer and another by the reader fails here.
     */
    private static Map<String, String> savedLines() {
        return TestinYml.lines("NAFATH", "https://github.com/acme/nafath-test-cases.git");
    }

    /**
     * {@code bugRepoUrl} is read like every other key, and the token a copied
     * address can carry is gone before anything holds it (#28).
     */
    @Test
    public void aBugRepositoryIsReadFromItsOwnKey() {
        final TestinProjectConfig config = TestinYml.parse(
                "testinProject: cases\nbugRepoUrl: https://mtb550:ghp_secret@github.com/mtb550/product.git\n", "bug repo");

        assertEquals(config.bugRepoUrl(), "https://github.com/mtb550/product.git", "a token never survives, as for RepoUrl");
        assertEquals(BugRepository.of(config.bugRepoUrl()).map(BugRepository::ghRepo).orElse(""), "github.com/mtb550/product");
    }

    /**
     * Left out, or naming no repository, there is nothing to file in - but what
     * the tester wrote stays, so the reason can say what is wrong with it.
     */
    @Test
    public void aBugRepoUrlThatNamesNoRepositoryIsKeptAndNotUsed() {
        assertTrue(BugRepository.of(TestinYml.parse("testinProject: cases\n", "none").bugRepoUrl()).isEmpty());
        assertEquals(TestinProjectConfig.EMPTY.bugRepoUrl(), "");

        final TestinProjectConfig wrong = TestinYml.parse(
                "bugRepoUrl: https://github.com/mtb550/product/issues\n", "issues page");
        assertEquals(wrong.bugRepoUrl(), "https://github.com/mtb550/product/issues");
        assertTrue(BugRepository.of(wrong.bugRepoUrl()).isEmpty());
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
        assertSame(TestinYml.parse("testinProject: [unclosed\n", "malformed"), TestinProjectConfig.EMPTY);
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
        final TestinYml.Parsed broken = TestinYml.parsed("testinProject: [unclosed\n", "malformed");
        final TestinYml.Parsed absent = TestinYml.parsed("   \n", "blank");

        assertEquals(broken.config(), absent.config(), "a file that would not parse has told us no more than one that is not there");

        assertFalse(broken.readable(), "the broken file is the one state a caller can act on differently");
        assertTrue(absent.readable(), "an absent file is not a broken one, and must not be reported as one");
    }

    /**
     * Rule-TREE-PANEL-117.
     * <p>
     * The file keeps the address it was given. It used to judge it as well, with
     * a narrower rule than the one thing that decides that, and dropped what it
     * refused with only a line in the log - so a testin.yml holding
     * {@code git://host/x} produced a panel that never mentioned an address at
     * all (#301).
     * <p>
     * What is refused is asked once, by {@code GitRefs.isRepositoryUrl}, where
     * something is about to clone it. {@code GitRefsTest} holds that half,
     * including the injected value this test used to pin.
     */
    @Test
    public void keepsTheRepoUrlItWasGiven() {
        assertEquals(TestinYml.parse("RepoUrl: \"https://x.com/r; rm -rf /\"\n", "injected").repoUrl(),
                "https://x.com/r; rm -rf /", "kept in the file, and never offered as a clone");
        assertEquals(TestinYml.parse("RepoUrl: file:///etc/passwd\n", "scheme").repoUrl(), "file:///etc/passwd");
        assertEquals(TestinYml.parse("RepoUrl: git@github.com:acme/cases.git\n", "ssh").repoUrl(),
                "git@github.com:acme/cases.git");
    }

    /**
     * The credential strip is not the judgement and did not move with it: the
     * file is committed, so a token that reached it would be in the repository's
     * history forever.
     */
    @Test
    public void stillTakesTheTokenOutOfARepoUrl() {
        assertEquals(TestinYml.parse("RepoUrl: https://ghp_secret@github.com/acme/cases.git\n", "token").repoUrl(),
                "https://github.com/acme/cases.git");
    }

    /**
     * The mode decides, not the addresses (#94).
     * <p>
     * The file can contradict itself - say local and still carry an address - so one
     * key is the authority and the rest is read against it. Otherwise something
     * has to choose which half of the file to believe.
     */
    @Test
    public void aProjectIsLocalUntilTheFileSaysOtherwise() {
        final TestinProjectConfig quiet = TestinYml.parse("testinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n",
                "an address and no location");

        assertEquals(quiet.location(), TestinLocation.LOCAL, "left out, it is local");
        assertFalse(quiet.hasRepoUrl());
    }

    @Test
    public void anAddressAloneDoesNotMakeAProjectRemote() {
        final TestinProjectConfig stillLocal = TestinYml.parse(
                "location: local\nRepoUrl: https://github.com/acme/cases.git\n", "local with an address left in");

        assertFalse(stillLocal.hasRepoUrl(), "local wins over everything below it");
    }

    /**
     * Git is the only way a project is shared, so {@code connection} says
     * nothing any more. A file written when it did still clones: the key is
     * skipped like any other unknown one, and what the file says around it
     * still counts.
     */
    @Test
    public void aFileThatStillSaysConnectionGitStillClones() {
        final TestinProjectConfig old = TestinYml.parse(
                "location: remote\nconnection: git\nRepoUrl: https://github.com/acme/cases.git\ntestinProject: cases\n", "old git");

        assertTrue(old.hasRepoUrl());
        assertEquals(old.projectName(), "cases");
    }

    /**
     * A file written for the SFTP server has no clone address, so it is not
     * shared, and the test project it names is still read.
     */
    @Test
    public void aFileThatStillSaysSftpIsReadAsNotShared() {
        final TestinProjectConfig old = TestinYml.parse("""
                location: remote
                connection: sftp
                sftpHost: qa.internal
                sftpPort: 2222
                sftpPath: /srv/testin
                testinProject: test-01
                """, "sftp");

        assertFalse(old.hasRepoUrl());
        assertEquals(old.projectName(), "test-01", "the server keys must not cost the file its project");
    }

    @Test
    public void aGitProjectIsReadFromItsUrl() {
        final TestinProjectConfig inGit = TestinYml.parse(
                "location: remote\ntestinProject: cases\nRepoUrl: https://github.com/acme/cases.git\n", "git");

        assertTrue(inGit.hasRepoUrl());
        assertEquals(inGit.projectName(), "cases");
    }

    @Test
    public void aWordNobodyCanReadIsLocalRatherThanAGuess() {
        assertEquals(TestinYml.parse("location: somewhere\n", "nonsense").location(),
                TestinLocation.LOCAL, "a project nobody can reach is better left on this machine");
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
        final TestinProjectConfig named = TestinYml.parse("""
                location: remote
                RepoUrl: https://github.com/mtb550/test-01.git
                testinProject: checkout
                """, "git");

        assertEquals(named.projectName(), "checkout", "the file says so, not the URL");
        assertTrue(named.hasRepoUrl());
    }

    @Test
    public void aUrlWithNoNameLeavesTheRepositoryUnbound() {
        final TestinProjectConfig unnamed = TestinYml.parse("""
                location: remote
                RepoUrl: https://github.com/mtb550/test-01.git
                """, "no name");

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

        assertEquals(TestinYml.parse("""
                        location: remote
                        testinProject: test-01
                        RepoUrl: https://mtb550:ghp_secret@github.com/mtb550/test-01.git
                        """, "token").repoUrl(), "https://github.com/mtb550/test-01.git",
                "stripped on the way in too, however it got there");
    }

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

    /**
     * Rule-TREE-PANEL-113, Rule-SHARE-004. A folder with no remote is local, a
     * remote's token never reaches the committed file, and without Git only the
     * project is written.
     */
    @Test
    public void theLinesSaySharedOnlyWithARemote() {
        assertEquals(TestinYml.lines("NAFATH", ""), Map.of("testinProject", "NAFATH", "location", "local"));
        assertEquals(TestinYml.lines("NAFATH", "https://ghp_secret@github.com/acme/nafath.git").get("RepoUrl"), "https://github.com/acme/nafath.git");
        assertEquals(TestinYml.lines("NAFATH"), Map.of("testinProject", "NAFATH"));
    }

    /**
     * UC-TREE-PANEL-029, Rule-TREE-PANEL-113. With no file, the file is the three
     * lines, and the reader reads back what was written.
     */
    @Test
    public void aNewFileIsTheThreeLines() {
        final String written = TestinYml.withLines("", savedLines());

        assertEquals(written, "testinProject: NAFATH\nlocation: remote\nRepoUrl: https://github.com/acme/nafath-test-cases.git\n");
        assertEquals(TestinYml.parse(written, "saved").projectName(), "NAFATH");
        assertTrue(TestinYml.parse(written, "saved").hasRepoUrl(), "and a colleague's first open can clone it");
    }

    /**
     * Rule-TREE-PANEL-114. A line the button owns is replaced where it is; a
     * missing one goes at the end; everything else - a comment, bugRepoUrl, a key
     * Testin does not know, a commented-out key - stays byte for byte.
     */
    @Test
    public void theRestOfTheFileIsKept() {
        final String before = """
                # Which test project this repository drives.
                testinProject: Checkout
                bugRepoUrl: https://github.com/acme/app
                location: local
                # testinProject: test-01       which test project, local or shared
                somethingFromALaterBuild: true
                """;

        assertEquals(TestinYml.withLines(before, savedLines()), """
                # Which test project this repository drives.
                testinProject: NAFATH
                bugRepoUrl: https://github.com/acme/app
                location: remote
                # testinProject: test-01       which test project, local or shared
                somethingFromALaterBuild: true
                RepoUrl: https://github.com/acme/nafath-test-cases.git
                """);
    }

    /**
     * A file saved on Windows keeps its line endings, and one that ended without
     * a newline keeps that ending when nothing is added.
     */
    @Test
    public void theFileKeepsItsLineEndings() {
        assertEquals(TestinYml.withLines("testinProject: Checkout\r\nlocation: local\r\n", Map.of("testinProject", "NAFATH")),
                "testinProject: NAFATH\r\nlocation: local\r\n");
        assertEquals(TestinYml.withLines("location: local\ntestinProject: Checkout", Map.of("testinProject", "NAFATH")),
                "location: local\ntestinProject: NAFATH");
        assertEquals(TestinYml.withLines("location: local\r\n", Map.of("testinProject", "NAFATH")),
                "location: local\r\ntestinProject: NAFATH\r\n", "an added line takes the file's ending too");
    }

    /**
     * A line added to a file that ended without a newline starts on a line of
     * its own, and the file then ends with one.
     */
    @Test
    public void aLineIsAddedAfterALastLineWithNoNewline() {
        assertEquals(TestinYml.withLines("location: remote", Map.of("testinProject", "NAFATH")),
                "location: remote\ntestinProject: NAFATH\n");
    }

    /**
     * Only a top-level key is the key: one nested under something else belongs
     * to that, and is left alone.
     */
    @Test
    public void aNestedKeyIsNotTheKey() {
        assertEquals(TestinYml.withLines("report:\n  testinProject: inner\n", Map.of("testinProject", "NAFATH")),
                "report:\n  testinProject: inner\ntestinProject: NAFATH\n");
    }

    /**
     * A key written twice has every line set: YAML reads the last one, so
     * leaving it would have Save say saved while the reader still found the old
     * name.
     */
    @Test
    public void aKeyWrittenTwiceIsSetEverywhere() {
        final String written = TestinYml.withLines("testinProject: Checkout\ntestinProject: Old\n", Map.of("testinProject", "NAFATH"));

        assertEquals(written, "testinProject: NAFATH\ntestinProject: NAFATH\n");
        assertEquals(TestinYml.parse(written, "twice").projectName(), "NAFATH");
    }

    /**
     * A test project's name is a folder name, and YAML reads some of those as
     * something else - a comment, a second key, a null. Each is quoted, so the
     * reader gets the name back as it was.
     */
    @Test
    public void aNameYamlWouldMisreadIsQuoted() {
        for (final String name : new String[]{"#1 Smoke", "Smoke #1", "Smoke:", "#1 O'Brien", "Tests: smoke", "null", "Yes", "- draft", "O'Brien's cases", "@home"}) {
            final String written = TestinYml.withLines("", Map.of("testinProject", name));
            assertEquals(TestinYml.parse(written, name).projectName(), name, "written as " + written.strip());
        }
        assertEquals(TestinYml.withLines("", Map.of("testinProject", "Nafath App")), "testinProject: Nafath App\n", "an ordinary name stays plain");
    }

    /**
     * UC-TREE-PANEL-029. What the preview compares against: each owned key's
     * value as the reader reads it - its quotes and a trailing comment are not
     * the value - and nothing for a key the file lacks, so a line already right
     * is shown as unchanged.
     */
    @Test
    public void thePreviewReadsWhatIsWritten() {
        final Map<String, String> values = TestinYml.valuesIn("# testinProject: old\ntestinProject: '#1 O''Brien'\nlocation: local  # for now\n",
                Set.of("testinProject", "location", "RepoUrl"));

        assertEquals(values, Map.of("testinProject", "#1 O'Brien", "location", "local"));
        assertEquals(TestinYml.valuesIn("testinProject: [unclosed\n", Set.of("testinProject")), Map.of(), "a file that does not parse has no values");
    }
}
