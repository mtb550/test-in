package org.testin.search;

import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * The rules behind the search (#29).
 * <p>
 * Two of them are worth pinning here rather than on screen.
 * <p>
 * A test case and a node come out of the search as the same shape, and
 * everything downstream depends on that: the tree is expanded to a node, the
 * editor for that node is opened, and a case is landed on inside it. If a case
 * did not carry the test set that holds it, going to one would have to ask what
 * kind of thing it was looking at, which is the branching this design exists to
 * avoid.
 * <p>
 * And the order, because a search that finds the right thing on line forty has
 * not found it. A tester recognizes a case by its description; one that matched
 * on a step or an id is a real hit and looks unrelated at a glance, so it goes
 * below the ones that explain themselves.
 */
public class SearchTest {

    private static TestSetDirectoryDto testSet(final String name, final String... chain) {
        final TestSetDirectoryDto set = TestSetDirectoryDto.builder().build();
        set.setName(name);
        set.setPath(Path.of("C:", "Testin", "test-01", "Test Cases", name));
        set.setPath2(new ArrayList<>(List.of(chain)));

        return set;
    }

    private static TestCaseDto testCase(final String description, final TestSetDirectoryDto in) {
        final TestCaseDto tc = TestCaseDto.builder().id(UUID.randomUUID()).description(description).build();
        tc.setParent(in);

        return tc;
    }

    @Test
    public void aTestCasePointsAtTheTestSetThatHoldsIt() {
        final TestSetDirectoryDto login = testSet("Login", "test-01", "Test Cases", "Login");
        final TestCaseDto tc = testCase("Sign in with a valid user", login);

        final Hit hit = Hit.of(tc);

        assertSame(hit.node(), login, "a case is not a node of its own, so the tree goes to its set");
        assertEquals(hit.testCase(), java.util.Optional.of(tc), "and the editor lands on the case");
        assertEquals(hit.name(), "Sign in with a valid user");
    }

    @Test
    public void aNodeCarriesItselfAndNoCase() {
        final TestSetDirectoryDto login = testSet("Login", "test-01", "Test Cases", "Login");

        final Hit hit = Hit.of(login);

        assertSame(hit.node(), login);
        assertTrue(hit.testCase().isEmpty(), "a node is selected and nothing more");
        assertEquals(hit.name(), "Login");
    }

    @Test
    public void everyHitSaysWhereItLives() {
        final TestSetDirectoryDto login = testSet("Login", "test-01", "Test Cases", "Auth", "Login");

        assertEquals(Hit.of(login).where(), "test-01 > Test Cases > Auth > Login");
        assertEquals(Hit.of(testCase("Sign in", login)).where(), "test-01 > Test Cases > Auth > Login",
                "three cases can be called the same thing in three sets, so the row has to say which");
    }

    @Test
    public void aQueryOfOneCharacterIsNotAQuery() {
        assertTrue(Hits.tooShort(""), "an empty field is not a search");
        assertTrue(Hits.tooShort("a"), "one letter matches almost the whole project");
        assertTrue(Hits.tooShort("  a  "), "and padding does not make it two");
        assertFalse(Hits.tooShort("ab"));
    }

    @Test
    public void matchingIgnoresCase() {
        assertTrue(Hits.contains("Sign in with a VALID user", "valid"));
        assertTrue(Hits.contains("Sign in", "SIGN"));
        assertFalse(Hits.contains("Sign in", "signed"));
    }

    @Test
    public void casesThatMatchOnTheirDescriptionComeFirst() {
        final TestSetDirectoryDto set = testSet("Login", "test-01");
        final TestCaseDto onADescription = testCase("Login with a valid user", set);
        final TestCaseDto onSomethingUnseen = testCase("Check the balance", set);

        final List<TestCaseDto> sorted = new ArrayList<>(List.of(onSomethingUnseen, onADescription));
        sorted.sort(Hits.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), onADescription,
                "the other one is a real hit - it matched on a step - but it looks unrelated at a glance");
    }

    @Test
    public void casesMatchingEquallyWellAreAlphabetical() {
        final TestSetDirectoryDto set = testSet("Login", "test-01");
        final TestCaseDto second = testCase("Login with a valid user", set);
        final TestCaseDto first = testCase("Login fails on a bad password", set);

        final List<TestCaseDto> sorted = new ArrayList<>(List.of(second, first));
        sorted.sort(Hits.byDescriptionMatchThenText("login"));

        assertSame(sorted.getFirst(), first);
    }

    @Test
    public void theNodeWhoseNameIsTheQueryOutranksOneThatMerelyContainsIt() {
        final TestSetDirectoryDto exact = testSet("Login", "test-01");
        final TestSetDirectoryDto longer = testSet("Login and logout journeys", "test-01");

        assertTrue(Hits.byClosestName(exact, longer) < 0,
                "typing a set's name should put that set at the top, not a sentence containing it");
    }

    @Test
    public void nodesOfTheSameLengthAreAlphabetical() {
        assertTrue(Hits.byClosestName(testSet("Alpha", "x"), testSet("Bravo", "x")) < 0);
    }

    /**
     * With nothing typed the search is a way of getting around, so what it lists
     * is what a tester can actually open. A package is somewhere to look through
     * rather than somewhere to go, and listing every one of them would be the
     * tree again, in a dialog.
     */
    @Test
    public void aTestSetIsSomewhereToGo() {
        assertTrue(testSet("Login", "test-01").isOpenableInEditor(),
                "the empty-query list is built from this, so a set that stopped saying yes would vanish from it");
    }

    @Test
    public void aPackageIsNot() {
        final TestSetPackageDirectoryDto pkg = TestSetPackageDirectoryDto.builder().build();
        pkg.setName("Auth");

        assertFalse(pkg.isOpenableInEditor(), "there is no editor to open, so Enter on it would do nothing");
    }

    @Test
    public void theEmptyQueryListReadsAsTheTreeDoes() {
        final TestSetDirectoryDto underAuth = testSet("Login", "test-01", "Test Cases", "Auth", "Login");
        final TestSetDirectoryDto underBilling = testSet("Invoices", "test-01", "Test Cases", "Billing", "Invoices");

        assertTrue(Hits.inTreeOrder(underAuth, underBilling) < 0,
                "Auth before Billing, so a project's sets stay together instead of interleaving alphabetically");
    }

    @Test
    public void twoNodesInOnePlaceAreAlphabetical() {
        final TestSetDirectoryDto second = testSet("Beta", "test-01", "Test Cases");
        final TestSetDirectoryDto first = testSet("Alpha", "test-01", "Test Cases");

        assertTrue(Hits.inTreeOrder(first, second) < 0);
    }

    /**
     * What the editor can be opened on, which is the question that crashed.
     * <p>
     * The editor type used to be read as "a test run, or else a test set", so
     * every other node - a package, the Test Cases folder, the Test Runs folder -
     * was opened as a test set and died casting itself to one. Typing "run" was
     * enough to find the Test Runs folder and press Enter on it.
     * <p>
     * The node declares it now, and these are the two that say yes. If a third
     * kind ever does, it needs an editor written for it, and this test is where
     * that is noticed.
     */
    @Test
    public void onlyTestSetsAndTestRunsHaveAnEditorToOpen() {
        assertTrue(testSet("Login", "test-01").isOpenableInEditor(), "a test set opens the test editor");
        assertTrue(TestRunDirectoryDto.builder().build().isOpenableInEditor(), "a run opens the run editor");
    }

    @Test
    public void aFolderOfNodesHasNoEditorAndIsNotOpened() {
        assertFalse(TestSetPackageDirectoryDto.builder().build().isOpenableInEditor());
        assertFalse(TestRunPackageDirectoryDto.builder().build().isOpenableInEditor());
        assertFalse(TestCasesMainDirectoryDto.builder().build().isOpenableInEditor(),
                "the Test Cases folder is somewhere to look through, not somewhere to open");
        assertFalse(TestRunsMainDirectoryDto.builder().build().isOpenableInEditor(),
                "and the Test Runs folder is the one that crashed");
        assertFalse(new TestProjectDirectoryDto().isOpenableInEditor(),
                "a project is where the two folders live, and neither it nor they open");
    }
}
