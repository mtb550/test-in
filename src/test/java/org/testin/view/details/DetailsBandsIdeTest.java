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
package org.testin.view.details;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.RunItemStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Bundle;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DetailsBandsIdeTest extends BasePlatformTestCase {

    private static final @NotNull String RUN = Bundle.message("details.band.run").toUpperCase(Locale.ROOT);
    private static final @NotNull String TEST_CASE = Bundle.message("details.band.test.case").toUpperCase(Locale.ROOT);
    private static final @NotNull UUID ID = UUID.fromString("3f2a05c1-8b44-4e2a-9f31-0c7d6b1a9c1b");
    private static final @NotNull String LAST_STEP = "Login";
    private static final @NotNull String STACKTRACE = """
            java.lang.AssertionError: expected [true]
                at one
                at two
                at three
                at four
                at five""";

    private static void collect(final @NotNull Container container, final @NotNull List<String> words) {
        for (final Component child : container.getComponents()) {
            add(words, text(child));
            if (child instanceof final Container inner) collect(inner, words);
        }
    }

    private static @NotNull String text(final @NotNull Component component) {
        return switch (component) {
            case final AbstractButton button -> words(button.getText());
            case final JLabel label -> words(label.getText());
            case final JTextComponent area -> words(area.getText());
            default -> "";
        };
    }

    private static @NotNull String words(final String text) {
        return Objects.requireNonNullElse(text, "");
    }

    private static void add(final @NotNull List<String> words, final @NotNull String text) {
        if (!text.isBlank()) words.add(text.trim());
    }

    private static boolean holds(final @NotNull List<String> words, final @NotNull String text) {
        return words.stream().anyMatch(word -> word.contains(text));
    }

    private static @NotNull TestCaseDto aTestCase() {
        return TestCaseDto.builder().id(ID).description("Log in with a valid user").expectedResult("The dashboard opens").module("Accounts").build();
    }

    private static @NotNull TestRunItems failed() {
        return TestRunItems.builder().id(ID).status(RunItemStatus.FAILED).actualResult("The session was dropped").duration(Duration.ofSeconds(134)).executedBy("muteb").stacktrace(STACKTRACE).build();
    }

    private @NotNull List<String> shown(final @NotNull Optional<TestRunItems> runItem) {
        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new DetailsTab().load(getProject(), tab, Optional.of(aTestCase()), runItem, List.of("Demo", "Test Cases", LAST_STEP));

        final @NotNull List<String> words = new ArrayList<>();
        collect(tab, words);
        return words;
    }

    public void testTheStacktraceIsALinkAndNotAValue() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the link that opens the stacktrace is missing: " + words, words.contains(Bundle.message("view.stacktrace.link")));
        assertFalse("the stacktrace itself was drawn on the panel: " + words, holds(words, "java.lang.AssertionError"));
    }

    public void testTheTestRunBandComesBeforeTheTestCaseBand() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the test run band was not drawn for a test case the test run holds: " + words, words.contains(RUN));
        assertTrue("the test case band was not drawn: " + words, words.contains(TEST_CASE));
        assertTrue("the test case band was drawn above the test run band", words.indexOf(RUN) < words.indexOf(TEST_CASE));
    }

    public void testATestCaseWithNoTestRunIsDrawnWithNoBandsAtAll() {
        final @NotNull List<String> words = shown(Optional.empty());

        assertFalse("the test run band was drawn with nothing to put in it: " + words, words.contains(RUN));
        assertFalse("a band's name was drawn over the only thing on the panel: " + words, words.contains(TEST_CASE));
        assertTrue("the test case's fields were not drawn: " + words, holds(words, "The dashboard opens"));
    }

    public void testThePanelShowsNoTestCaseId() {
        assertFalse("the panel is still drawing the id", shown(Optional.empty()).contains(ID.toString()));
    }

    public void testTheTestCaseBandIsFoldedWhereATestRunStandsAboveIt() {
        final @NotNull List<String> words = shown(Optional.of(failed()));

        assertTrue("the test case band's name was not drawn: " + words, words.contains(TEST_CASE));
        assertFalse("a test case field was drawn while the band was folded: " + words, holds(words, "The dashboard opens"));
        assertFalse("a test case field was drawn while the band was folded: " + words, holds(words, "Accounts"));
    }
}
