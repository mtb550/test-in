package org.testin.testcase.updateDialog.bulk;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
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
    private final @Nullable Consumer<List<TestCaseDto>> updatedItems;
    private final @NotNull BulkJsonEditors editors;

    /**
     * The escaped text each value started as, by index. The editor shows
     * newlines flattened to spaces, so this is what an untouched value looks
     * like on screen - not what is in storage.
     */
    private final @NotNull List<String> originalEscaped = new ArrayList<>();

    protected JsonSplitBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems,
                                         final @Nullable Consumer<List<TestCaseDto>> updatedItems) {
        super(p);
        this.selectedItems = selectedItems;
        this.updatedItems = updatedItems;

        title = getPopupTitle();

        editors = new BulkJsonEditors(p);
        buildContent();

        components = List.of(ComponentDialogBase.of(editors));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit),
                StatusBarShortcut.build(Shortcuts.TabNext, "Next", () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.TabPrevious, "Previous", () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowDown, "Next", () -> editors.navigate(1, false)),
                StatusBarShortcut.build(Shortcuts.ArrowUp, "Previous", () -> editors.navigate(-1, false)),
                StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, "All Carets", editors::caretOnEveryValue),
                StatusBarShortcut.hint("Ctrl+Click", "Multi-Caret"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

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

    protected abstract @NotNull String getOriginalValue(final @NotNull TestCaseDto tc);

    /**
     * Applies one edited (non-null, trimmed) value to the test case.
     */
    protected abstract void setValue(final @NotNull TestCaseDto tc, final @NotNull String value);

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

    protected void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<String> newValues) {
        for (int i = 0; i < items.size(); i++) {
            final String raw = newValues.get(i);
            if (raw == null) continue; // unchanged row - never rewrite (see submit)

            final String value = raw.trim();
            if (value.isEmpty() && !acceptsBlank()) continue;

            setValue(items.get(i), value);
        }
    }

    // ------------------------------------------------------------------
    // The dialog.
    // ------------------------------------------------------------------

    /**
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

    @Override
    protected void submit() {
        final List<String> newValues = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            final String current = editors.valueAt(i);
            // The editor shows newlines flattened to spaces, so writing an
            // untouched row back would permanently flatten a stored multi-line
            // value. null = "unchanged, skip".
            newValues.add(current == null || current.equals(originalEscaped.get(i))
                    ? null
                    : BulkJsonEditor.unescapeJson(current).trim());
        }

        applyValues(selectedItems, newValues);
        // todo, apply update automation edit bulk test cases. set to null for now
        if (updatedItems != null) updatedItems.accept(selectedItems);

        closeOk();
    }

    /**
     * Both sides of the pair, and the ranges of the right one that may be typed
     * into. The two texts are identical up to the values themselves.
     */
    private void buildContent() {
        final StringBuilder left = new StringBuilder("[\n");
        final StringBuilder right = new StringBuilder("[\n");
        final List<int[]> editableRanges = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            final TestCaseDto tc = selectedItems.get(i);
            final String escapedValue = BulkJsonEditor.escapeJson(getOriginalValue(tc));
            originalEscaped.add(escapedValue);

            final StringBuilder prefixSb = new StringBuilder("  {\n    \"id\": \"")
                    .append(BulkJsonEditor.escapeJson(tc.getId().toString())).append("\",\n");
            if (showsDescriptionContext()) {
                prefixSb.append("    \"description\": \"").append(BulkJsonEditor.escapeJson(tc.getDescription())).append("\",\n");
            }
            prefixSb.append("    \"").append(getJsonFieldName()).append("\": \"");

            final String prefix = prefixSb.toString();
            final String suffix = "\"\n  }";
            final String comma = (i == selectedItems.size() - 1) ? "\n" : ",\n";

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
