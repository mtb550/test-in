package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bulk-edits one value per test case: the original JSON on the left, an
 * editable copy on the right where only that value can be typed into.
 * <p>
 * A framework dialog - the title, the status bar and the key bindings come from
 * the declaration below, so a shortcut cannot be shown without working.
 */
public abstract class JsonSplitBulkSectionDialog extends AbstractFrameworkDialog<BulkJsonEditors> {

    private final @NotNull List<TestCaseDto> selectedItems;
    private final @NotNull Consumer<List<TestCaseDto>> updatedItems;
    private final @NotNull BulkJsonEditors editors;

    /**
     * The escaped text each value started as, by index - what an untouched
     * value looks like on screen, which is not what is in storage: a line break
     * is shown as the two characters that stand for it.
     */
    private final @NotNull List<String> originalEscaped = new ArrayList<>();

    protected JsonSplitBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems, final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p);
        this.selectedItems = selectedItems;
        this.updatedItems = updatedItems;

        title = getPopupTitle();

        editors = new BulkJsonEditors(p);
        buildContent();

        components = List.of(ComponentDialogBase.of(editors));

        shortcuts = List.of(
                StatusBarShortcut.save(this::submit),
                StatusBarShortcut.hint("Shift+Enter", StatusBarShortcut.SAVE),
                StatusBarShortcut.build(Shortcuts.TabNext, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.TabPrevious, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowDown, Bundle.message("bulk.shortcut.next"), () -> editors.navigate(1, false)),
                StatusBarShortcut.build(Shortcuts.ArrowUp, Bundle.message("bulk.shortcut.previous"), () -> editors.navigate(-1, false)),
                StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, Bundle.message("bulk.shortcut.all.carets"), editors::caretOnEveryValue),
                StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.multi.caret")),
                StatusBarShortcut.cancel(this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(1000), JBUI.scale(450));

        // The editor listens to the action system, not to Swing key bindings.
        editors.bindKeysToEditor(shortcuts);
    }

    // ------------------------------------------------------------------
    // What a concrete section supplies.
    // ------------------------------------------------------------------

    protected abstract @NotNull String getPopupTitle();

    /**
     * JSON key of the edited field, e.g. "testData".
     */
    protected abstract @NotNull String getJsonFieldName();


    /**
     * UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-039.
     * <p>
     * Which field of a test case this dialog edits. The two questions a bulk
     * edit asks of it - what is in there now, and write this back - are the two
     * {@link TestEditorAttributes} already answers for the grid, the import and
     * the update menu.
     * <p>
     * The seven dialogs answered them themselves until #176, and one of the
     * seven had drifted: a description typed here went in unsanitized, while the
     * same description typed into a grid cell or imported from a sheet did not.
     * Nothing failed - the two just stopped agreeing about what a description is.
     */
    protected abstract @NotNull TestEditorAttributes attribute();

    /**
     * What the field holds now, as the tester will see it in the JSON - the raw
     * value, because this is an editable surface and a formatted one would be
     * committed back (Rule-EDITOR-PANEL-005).
     */
    protected @NotNull String getOriginalValue(final @NotNull TestCaseDto tc) {
        return attribute().gridValue(tc);
    }

    /**
     * Writes what the tester typed, through the one setter that owns the field:
     * it sanitizes where the field is sanitized, parses where it is parsed, and
     * refuses a word it cannot read by leaving the case as it was.
     */
    protected boolean setValue(final @NotNull TestCaseDto tc, final @NotNull String value) {
        return attribute().getImportSetter().execute(p, tc, value);
    }

    /**
     * Whether a value edited to blank may be applied (e.g. a description must not be blanked).
     */
    protected boolean acceptsBlank() {
        return true;
    }

    /**
     * Whether the description is rendered as read-only context above the edited field.
     * False when the edited field IS the description.
     */
    protected boolean showsDescriptionContext() {
        return true;
    }

    /**
     * UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-041.
     * <p>
     * Writes the rows the tester edited, and answers with the cases it actually
     * wrote to.
     * <p>
     * The answer is the point. This has always skipped the untouched rows - the
     * caller then handed the whole selection on to be saved, regenerated and
     * counted, so fifty cases were written to disk and their automation
     * rebuilt because one of them was edited, and the tester was told fifty had
     * changed. Two rules for what a bulk edit touched, in two places, and only
     * one of them was right.
     * <p>
     * A row Testin could not read is not one of them either. It is left as it
     * was, kept out of the answer so the count does not claim it, and said once
     * with the others - Rule-EDITOR-PANEL-206, which the grid and the two
     * importers already kept and this dialog did not (#295).
     */
    protected @NotNull List<TestCaseDto> applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<EditedValue> newValues) {
        final @NotNull List<TestCaseDto> written = new ArrayList<>();

        int refused = 0;

        for (int i = 0; i < items.size(); i++) {
            final @NotNull EditedValue edited = newValues.get(i);
            if (!edited.changed()) continue;

            // Counted, not passed over. A field that will not take a blank is
            // refusing the value the tester typed, and skipping it silently
            // closed the dialog with nothing changed and nothing said - three
            // descriptions cleared to blank looked exactly like three saved
            // (#66, finding 81).
            if (edited.value().isEmpty() && !acceptsBlank()) {
                refused++;
                continue;
            }

            if (!setValue(items.get(i), edited.value())) {
                refused++;
                continue;
            }

            written.add(items.get(i));
        }

        TestEditorAttributes.sayWhatWasRefused(p, refused);

        return written;
    }

    // ------------------------------------------------------------------
    // The dialog.
    // ------------------------------------------------------------------

    /**
     * UC-EDITOR-PANEL-007.
     * <p>
     * Shows the dialog and releases the editors when it closes. The framework
     * creates the popup inside show(), so the close listener is attached after.
     */
    public void open() {
        show();
        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                editors.release();
            }
        });
        editors.focusFirstValue();
    }

    // UC-EDITOR-PANEL-007, Rule-EDITOR-PANEL-042
    @Override
    protected void submit() {
        final @NotNull List<EditedValue> newValues = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            // A row reading the same as it started was not edited, and neither
            // was one the editor cannot read back - both are left as they are.
            final int index = i;
            newValues.add(editors.valueAt(index)
                    .filter(current -> !current.equals(originalEscaped.get(index)))
                    .map(current -> EditedValue.of(BulkJsonEditor.unescapeJson(current).trim()))
                    .orElse(EditedValue.UNCHANGED));
        }

        // Only the cases that were written. Handing on the whole selection
        // saved and regenerated every one of them and reported a count nobody
        // had earned.
        updatedItems.accept(applyValues(selectedItems, newValues));

        closeOk();
    }

    /**
     * Both sides of the pair, and the ranges of the right one that may be typed
     * into. The two texts are identical up to the values themselves.
     */
    private void buildContent() {
        final @NotNull StringBuilder left = new StringBuilder("[\n");
        final @NotNull StringBuilder right = new StringBuilder("[\n");
        final @NotNull List<int[]> editableRanges = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            final @NotNull TestCaseDto tc = selectedItems.get(i);
            final @NotNull String escapedValue = BulkJsonEditor.escapeJson(getOriginalValue(tc));
            originalEscaped.add(escapedValue);

            final @NotNull StringBuilder prefixSb = new StringBuilder("  {\n    \"id\": \"")
                    .append(BulkJsonEditor.escapeJson(tc.getId().toString())).append("\",\n");
            if (showsDescriptionContext()) {
                prefixSb.append("    \"description\": \"").append(BulkJsonEditor.escapeJson(tc.getDescription())).append("\",\n");
            }
            prefixSb.append("    \"").append(getJsonFieldName()).append("\": \"");

            final @NotNull String prefix = prefixSb.toString();
            final @NotNull String suffix = "\"\n  }";
            final @NotNull String comma = (i == selectedItems.size() - 1) ? "\n" : ",\n";

            left.append(prefix).append(escapedValue).append(suffix).append(comma);

            right.append(prefix);
            final int start = right.length();
            right.append(escapedValue);
            editableRanges.add(new int[]{start, right.length()});
            right.append(suffix).append(comma);
        }

        left.append("]");
        right.append("]");

        editors.setContent(left.toString(), right.toString(), editableRanges);
        editors.setOriginalTextSource(originalEscaped::get);
    }
}
