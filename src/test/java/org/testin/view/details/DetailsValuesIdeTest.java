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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.editor.ShownFields;
import org.testin.indexer.TestRuns;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseEditorAttributes;
import org.testin.testrun.TestRunEditorAttributes;
import org.testin.ui.framework.Picture;
import org.testin.ui.framework.ShownDialog;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DetailsValuesIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String ORDER = TestCaseEditorAttributes.ORDER.getName().toUpperCase(Locale.ROOT);

    private static @NotNull TestCaseDto everyFieldFilled(final @NotNull TestCaseDto tc) {
        tc.setExpectedResult("The dashboard opens");
        tc.setSteps(List.of("Open the login page", "Type the credentials"));
        tc.setPreConditions("An account exists");
        tc.setTestData("user=admin");
        tc.setReference("JIRA-12");
        tc.setModule("Accounts");
        return tc;
    }

    private static @NotNull TestRunItems failedWith(final @NotNull TestCaseDto tc, final @NotNull List<String> screenshots) {
        return TestRunItems.builder().id(tc.getId()).status(RunItemStatus.FAILED).actualResult("The session was dropped").duration(Duration.ofSeconds(134)).executedBy("muteb")
                .bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).stacktrace("java.lang.AssertionError: expected [true]").screenshots(screenshots).build();
    }

    private static byte @NotNull [] aScreenshot(final int width, final int height) {
        return Picture.toPng(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
    }

    private static @NotNull Container lineOf(final @NotNull Component part) {
        return part.getParent();
    }

    private @NotNull JBPanel<?> drawnWithEveryBandOpen(final @NotNull TestCaseDto tc, final @NotNull Optional<TestRunItems> runItem, final @NotNull List<String> path) {
        final boolean wasOpen = PropertiesComponent.getInstance().getBoolean(DetailsTab.TEST_CASE_OPEN, false);
        PropertiesComponent.getInstance().setValue(DetailsTab.TEST_CASE_OPEN, true, false);
        try {
            return Drawn.detailsTab(getProject(), tc, runItem, path);
        } finally {
            PropertiesComponent.getInstance().setValue(DetailsTab.TEST_CASE_OPEN, wasOpen, false);
        }
    }

    private @NotNull String orderIn(final @NotNull List<String> words) {
        final int at = words.indexOf(ORDER);
        assertTrue("no " + ORDER + " row was drawn: " + words, at >= 0 && at + 1 < words.size());
        return words.get(at + 1);
    }

    // Rule-VIEW-PANEL-028
    public void testNoValueCanBeTypedIntoOrTakesTheKeyboard() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto tc = everyFieldFilled(aTestCase(ts, "Log in with a valid user", "a"));
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(failedWith(tc, List.of())));
        final @NotNull JBPanel<?> tab = drawnWithEveryBandOpen(tc, Services.getInstance(getProject(), TestRuns.class).findTestRun(tr.getPath()).flatMap(run -> run.resultOf(tc.getId())), tr.getPath2());

        final @NotNull List<Component> drawn = Drawn.components(tab);
        assertTrue("the test case's own band was not drawn open: " + Drawn.words(tab), Drawn.holds(Drawn.words(tab), "The dashboard opens"));
        drawn.stream().filter(JTextComponent.class::isInstance).map(JTextComponent.class::cast)
                .forEach(value -> assertFalse("the value \"" + value.getText() + "\" can be typed into", value.isEditable()));

        tab.addNotify();
        try {
            final @NotNull KeyboardStop stop = new KeyboardStop();
            final @NotNull List<String> takers = drawn.stream().filter(stop::accept).map(component -> component.getClass().getSimpleName() + " \"" + Drawn.text(component) + "\"").toList();
            assertEquals("a value on the Details tab takes the keyboard from the tab", List.of(), takers);
        } finally {
            tab.removeNotify();
        }
    }

    // Rule-VIEW-PANEL-041
    public void testTheBreadcrumbIsOneStepForEachFolderAboveTheTestCase() {
        final @NotNull List<String> path = List.of("Demo", "Test Cases", "Accounts", "Login");
        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), TestCaseDto.builder().description("Log in with a valid user").build(), Optional.empty(), path);

        final @NotNull Container breadcrumb = lineOf(Drawn.reading(tab, "Demo"));
        final @NotNull List<JLabel> parts = Arrays.stream(breadcrumb.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).toList();
        final @NotNull List<String> steps = parts.stream().map(part -> Objects.toString(part.getText(), "")).filter(text -> !text.isEmpty()).toList();
        final long arrows = parts.stream().filter(part -> Optional.ofNullable(part.getIcon()).isPresent() && Objects.toString(part.getText(), "").isEmpty()).count();

        assertEquals("the breadcrumb does not show one step for each folder, in order", path, steps);
        assertEquals("the steps are not joined by one arrow between each two", path.size() - 1, arrows);

        final @NotNull JBPanel<?> noPath = Drawn.detailsTab(getProject(), TestCaseDto.builder().description("Log in with a valid user").build(), Optional.empty(), List.of());
        assertFalse("a panel handed no path drew a step: " + Drawn.words(noPath), Drawn.holds(Drawn.words(noPath), "Demo"));
    }

    // Rule-VIEW-PANEL-062
    public void testOrderIsWhereTheTestCaseSitsInItsTestSet() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestCaseDto middle = aTestCase(ts, "Log in with a locked user", "m");
        aTestCase(ts, "Log in with no password", "z");

        assertEquals("Order was not read as the test case's place in its test set", "2", orderIn(Drawn.words(Drawn.detailsTab(getProject(), middle, Optional.empty(), ts.getPath2()))));

        aTestCase(ts, "Log in twice", "b");
        assertEquals("Order did not follow the test set when a test case was put above it", "3", orderIn(Drawn.words(Drawn.detailsTab(getProject(), middle, Optional.empty(), ts.getPath2()))));
    }

    // Rule-VIEW-PANEL-081
    public void testTheStacktraceLinkComesFirstThenOneThumbnailForEachScreenshot() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto tc = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(TestRunItems.builder().id(tc.getId()).build()));
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        final @NotNull List<String> names = testRuns.storeScreenshots(tr.getPath(), List.of(aScreenshot(320, 200), aScreenshot(640, 400)));
        assertEquals(2, names.size());

        final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), tc, Optional.of(failedWith(tc, names)), tr.getPath2());
        final @NotNull Component link = Drawn.reading(tab, Bundle.message("view.stacktrace.link"));
        final @NotNull List<Component> line = Arrays.asList(lineOf(link).getComponents());

        assertEquals("the line does not hold the link and one thumbnail for each screenshot", 1 + names.size(), line.size());
        assertSame("the Stacktrace link is not first on its line", link, line.getFirst());
        assertTrue("the link is not a link", link instanceof ActionLink);
        for (int i = 0; i < names.size(); i++) {
            final @NotNull JBLabel thumbnail = (JBLabel) line.get(i + 1);
            final @NotNull String name = names.get(i);
            assertTrue("hovering a thumbnail does not name its file: " + thumbnail.getToolTipText(), String.valueOf(thumbnail.getToolTipText()).contains(name));
            Await.until("the thumbnail of " + name + " was never drawn 48 pixels high", () -> heightOf(thumbnail.getIcon()) == 48 && widthOf(thumbnail.getIcon()) > 48);
        }

        final @NotNull JBLabel second = (JBLabel) line.get(2);
        final @NotNull MouseEvent click = new MouseEvent(second, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1);
        for (final MouseListener listener : second.getMouseListeners()) listener.mouseClicked(click);
        try {
            final @NotNull JComponent shown = ShownDialog.content(getProject(), ScreenshotDialog.class);
            final @NotNull Icon picture = Drawn.components(shown).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast).map(JBLabel::getIcon).filter(icon -> widthOf(icon) > 48).findFirst()
                    .orElseThrow(() -> new AssertionError("the screenshot's window shows no picture"));
            assertEquals("the screenshot did not open at its real width", 640, picture.getIconWidth());
            assertEquals("the screenshot did not open at its real height", 400, picture.getIconHeight());
        } finally {
            ShownDialog.close(getProject(), ScreenshotDialog.class);
        }
    }

    // Rule-VIEW-PANEL-103, Rule-VIEW-PANEL-081
    public void testTheStacktraceAndScreenshotsAreDrawnWhenFieldsHidesTheStacktrace() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        final @NotNull TestCaseDto tc = aTestCase(ts, "Log in with a valid user", "a");
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(TestRunItems.builder().id(tc.getId()).build()));
        final @NotNull List<String> names = Services.getInstance(getProject(), TestRuns.class).storeScreenshots(tr.getPath(), List.of(aScreenshot(320, 200)));
        final @NotNull Set<TestRunEditorAttributes> was = ShownFields.read(ShownFields.IN_TEST_RUNS, TestRunEditorAttributes.class);
        final @NotNull Set<TestRunEditorAttributes> chosen = EnumSet.allOf(TestRunEditorAttributes.class);
        chosen.remove(TestRunEditorAttributes.STACKTRACE);
        ShownFields.write(ShownFields.IN_TEST_RUNS, chosen);
        try {
            final @NotNull JBPanel<?> tab = Drawn.detailsTab(getProject(), tc, Optional.of(failedWith(tc, names)), tr.getPath2());

            assertTrue("the Stacktrace link went missing because Fields hides the Stacktrace", Drawn.words(tab).contains(Bundle.message("view.stacktrace.link")));
            assertTrue("the screenshot was not drawn because Fields hides the Stacktrace", Drawn.components(tab).stream().filter(JBLabel.class::isInstance).map(JBLabel.class::cast).anyMatch(label -> Drawn.hovering(label).contains(names.getFirst())));
        } finally {
            ShownFields.write(ShownFields.IN_TEST_RUNS, was);
        }
    }

    private static int heightOf(final Icon icon) {
        return Optional.ofNullable(icon).map(Icon::getIconHeight).orElse(0);
    }

    private static int widthOf(final Icon icon) {
        return Optional.ofNullable(icon).map(Icon::getIconWidth).orElse(0);
    }

    private static final class KeyboardStop extends LayoutFocusTraversalPolicy {
        @Override
        public boolean accept(final @NotNull Component component) {
            return super.accept(component);
        }
    }
}
