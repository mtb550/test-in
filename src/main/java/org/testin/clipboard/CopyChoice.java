package org.testin.clipboard;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestEditorAttributes;
import org.testin.model.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.MenuItem;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * UC-EDITOR-PANEL-014, Rule-EDITOR-PANEL-207.
 * <p>
 * One row of the copy menu: a value of the test case, and the letter that puts
 * it on the clipboard.
 * <p>
 * Its own type rather than the update menu's {@code UpdateTestCaseFields}, which
 * is the same shape and not the same thing. This menu offers the class name, the
 * identity and the path - values a tester copies into a ticket or a stack trace
 * and can never edit - so one enum over both would have put rows in the update
 * menu that mean nothing there, or left them out of the menu that exists for
 * them.
 * <p>
 * What the two share is where the parts live: the keystroke is in
 * {@link Shortcuts}, the caption is the attribute's own, and the popup is
 * {@code ShortcutMenuPopup}. The letters match the update menu's wherever the
 * field is the same, so D is the description in both.
 */
@Getter
public enum CopyChoice implements MenuItem {

    /**
     * Everything the tester wrote, which is what CTRL+C did on its own before
     * this menu existed - so it is the first row and the one already selected,
     * and CTRL+C then ENTER is the gesture it always was.
     */
    ALL_DETAILS(Bundle.message("copy.all.details"), Shortcuts.CopyAll),

    DESCRIPTION(TestEditorAttributes.DESCRIPTION, Shortcuts.CopyDescription),
    EXPECTED_RESULT(TestEditorAttributes.EXPECTED_RESULT, Shortcuts.CopyExpectedResult),
    STEPS(TestEditorAttributes.STEPS, Shortcuts.CopySteps),
    PRE_CONDITIONS(TestEditorAttributes.PRE_CONDITIONS, Shortcuts.CopyPreConditions),
    TEST_DATA(TestEditorAttributes.TEST_DATA, Shortcuts.CopyTestData),
    PRIORITY(TestEditorAttributes.PRIORITY, Shortcuts.CopyPriority),
    MODULE(TestEditorAttributes.MODULE, Shortcuts.CopyModule),
    GROUP(TestEditorAttributes.GROUP, Shortcuts.CopyGroup),
    STATUS(TestEditorAttributes.STATUS, Shortcuts.CopyStatus),
    REFERENCE(TestEditorAttributes.REFERENCE, Shortcuts.CopyReference),

    /**
     * The three the update menu cannot offer, and the reason this enum is not
     * that one. None of them is written by a tester - Testin derives all three -
     * and all three are what somebody pastes into a stack trace, a ticket or a
     * command line.
     */
    FQCN(TestEditorAttributes.FQCN, Shortcuts.CopyFqcn),
    ID(TestEditorAttributes.ID, Shortcuts.CopyId),
    PATH(TestEditorAttributes.PATH, Shortcuts.CopyPath);

    /**
     * One icon for every row. They all do the same thing to different values, so
     * a picture each would be decoration where the caption is the answer.
     */
    private static final @NotNull Icon ICON = AllIcons.Actions.Copy;

    private final @NotNull String name;
    private final @NotNull Shortcuts shortcut;

    /**
     * The value this row copies, and empty for the row that copies all of them.
     * Empty rather than a null, so no reader tests for one.
     */
    private final @NotNull Optional<TestEditorAttributes> attribute;

    CopyChoice(final @NotNull TestEditorAttributes attribute, final @NotNull Shortcuts shortcut) {
        this.name = attribute.getName();
        this.shortcut = shortcut;
        this.attribute = Optional.of(attribute);
    }

    CopyChoice(final @NotNull String name, final @NotNull Shortcuts shortcut) {
        this.name = name;
        this.shortcut = shortcut;
        this.attribute = Optional.empty();
    }

    /**
     * What this row puts on the clipboard for one test case.
     * <p>
     * A single value is copied bare, with no caption: a tester who asked for the
     * class name wants to paste the class name, not "FQCN: " and then it. The
     * whole test case keeps its captions, because there it is the captions that
     * make it readable.
     */
    public @NotNull String from(final @NotNull TestCaseDto tc) {
        return attribute.map(one -> one.gridValue(tc)).orElseGet(() -> allDetailsOf(tc));
    }

    /**
     * Every field the tester wrote, captioned, one to a line - and a field left
     * empty is not a line (#197).
     */
    private static @NotNull String allDetailsOf(final @NotNull TestCaseDto tc) {
        return Arrays.stream(TestEditorAttributes.values())
                .filter(attr -> attr.can(Can.COPY))
                .filter(attr -> !attr.gridValue(tc).isBlank())
                // The colon belongs to this line, not to the caption. It used to be
                // part of the name, so the view panel drew test case rows with
                // one and run rows without in the same column (#232).
                .map(attr -> attr.getName() + ": " + attr.gridValue(tc))
                .collect(Collectors.joining("\n"));
    }

    /**
     * What the tester is told afterward. The value, not the count of characters:
     * "Description copied", and "Description copied 3" for a selection - the same
     * shape every other copy in the plugin uses.
     */
    public @NotNull String copiedMessage(final int cases) {
        return cases == 1
                ? Bundle.message("copy.done.one", name)
                : Bundle.message("copy.done.many", name, String.valueOf(cases));
    }

    @Override
    public @NotNull Icon getIcon() {
        return ICON;
    }

    @Override
    public @NotNull String getShortcutText() {
        return shortcut.getShortcutText();
    }

    /**
     * Makes this row's letter pick it while the menu is showing.
     * <p>
     * The same binding {@code UpdateTestCaseFields} does for the update menu, and
     * the reason it is here rather than in the popup: a row's key belongs to the
     * row.
     */
    @Override
    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onTrigger) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onTrigger.run();
            }
        }.registerCustomShortcutSet(shortcut.getCustomShortcut(), component);
    }

    /**
     * The whole menu for a selection: every row's value for every case, blocks
     * separated by a blank line so a multi-case copy reads as separate answers
     * and not one run-on.
     */
    public @NotNull String from(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().map(this::from).collect(Collectors.joining("\n\n"));
    }
}
