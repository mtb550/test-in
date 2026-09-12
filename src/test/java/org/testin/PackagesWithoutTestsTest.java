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

package org.testin;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Every top-level package either has tests or says here why it has none (#108).
 * <p>
 * The count on its own says nothing - nineteen packages were untested when that
 * issue was written and most of them were actions, which a unit test cannot
 * reach. What is worth knowing is which ones were <b>decided</b> and which ones
 * nobody has looked at, and there is no way to tell those apart from a number.
 * <p>
 * So a package with no tests is listed below with the reason, and one that is
 * not listed fails this test. Adding a package is then a question - what would
 * a test of this say? - asked once, by the build, rather than never.
 * <p>
 * Recorded rather than quietly allowed, which is the same argument
 * {@code ArchitectureTest} makes for its own exception lists.
 */
public class PackagesWithoutTestsTest {

    private static final @NotNull Path MAIN = Path.of("src", "main", "java", "org", "testin");
    private static final @NotNull Path TEST = Path.of("src", "test", "java", "org", "testin");

    /**
     * Why each of these has no test of its own, measured 11 September 2026.
     * <p>
     * Two kinds of answer, and both are reasons rather than excuses: the logic
     * is already covered from somewhere else, or what the package does is
     * something only a running IDE can do - open a dialog, delete through the
     * VFS, paint a window. The second kind names the route that would reach it,
     * because "it needs an IDE" stopped being the end of the sentence when
     * {@code ideTest} arrived with this issue.
     */
    private static final @NotNull Map<String, String> WITHOUT_TESTS = Map.of(
            "creator",
            "NodeCreators is checked by NodeKindTablesTest; what is left is four dialogs and the actions that open them - ideTest",

            "remove",
            "Removals is checked by NodeKindTablesTest; RemoveAction is a confirmation and a VFS delete - ideTest",

            "notifications",
            "Done and Refused are resolved by BundleKeysTest.theNotificationVocabularyResolves; Notifier only hands them to the platform",

            "navigate",
            "an extension point whose in-core answer is the empty one; the implementation that navigates lives in testin-java and needs that plugin",

            "lightmode",
            "one window that paints, animates and reads the run editor beside it - a sandbox pass, not an assertion",

            "open",
            "two actions that open the node the tester selected, through the editors and the tool window - ideTest",

            "order",
            "an action and a dialog: the number a tester types goes to the marker through the indexer - ideTest",

            "rename",
            "an action, a dialog and the order the three steps run in - close the editor, rewrite the code, rename the node. Only a running IDE has all three - ideTest",

            "testproject",
            "creating, cloning, binding and selecting a test project: every one of them is a dialog over the indexer - ideTest");

    @Test
    public void everyPackageWithoutTestsSaysWhy() {
        final @NotNull Set<String> untested = new TreeSet<>(topLevel(MAIN));
        untested.removeAll(topLevel(TEST));

        assertEquals(untested, new TreeSet<>(WITHOUT_TESTS.keySet()),
                "the packages with no tests are not the ones this file records. Left is what the tree holds,"
                        + " right is what was decided: write a test for the new one, or add it above with the reason (#108)");
    }

    /**
     * A reason that says nothing is worse than no reason, because it reads like
     * a decision.
     */
    @Test
    public void everyReasonSaysSomething() {
        WITHOUT_TESTS.forEach((pkg, reason) -> {
            if (reason.length() < 40) fail(pkg + " is recorded as untested with a reason too short to be one: " + reason);
        });
    }

    /**
     * The top-level packages under {@code org.testin} that hold Java, at any
     * depth. A directory with no Java in it is a leftover rather than a package
     * - {@code git} does not track one, so it is on this machine only.
     */
    private static @NotNull Set<String> topLevel(final @NotNull Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(root::relativize)
                    .filter(relative -> relative.getNameCount() > 1)
                    .map(relative -> relative.getName(0).toString())
                    .collect(Collectors.toCollection(TreeSet::new));

        } catch (final IOException ex) {
            throw new AssertionError("Could not read " + root.toAbsolutePath() + ", so nothing was checked", ex);
        }
    }
}
