package org.testin.testcase.update.bulk;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bulk-edits a list of values per test case - steps, groups - with the original
 * JSON on the left and an editable copy on the right.
 * <p>
 * Unlike the single-value dialog this one rebuilds its text: adding or removing
 * an item changes how many editable spans there are, so the document, the
 * markers and the guarded blocks are all made again.
 */
public abstract class JsonArraySplitBulkSectionDialog extends AbstractFrameworkDialog<BulkJsonEditors> {

    private final @NotNull List<TestCaseDto> selectedItems;
    private final @NotNull Consumer<List<TestCaseDto>> updatedItems;
    private final @NotNull BulkJsonEditors editors;

    private final @NotNull List<List<String>> originalValues = new ArrayList<>();
    private final @NotNull List<List<String>> activeValues = new ArrayList<>();

    /**
     * Which test case and which of its items each editable span belongs to, in
     * the order the spans were written. Rebuilt with the text.
     */
    private final @NotNull List<int[]> spanOwners = new ArrayList<>();

    protected JsonArraySplitBulkSectionDialog(final @NotNull Project p, final @NotNull List<TestCaseDto> selectedItems,
                                              final @NotNull Consumer<List<TestCaseDto>> updatedItems) {
        super(p);
        this.selectedItems = selectedItems;
        this.updatedItems = updatedItems;

        title = getPopupTitle();

        for (final List<String> values : extractOriginalValues(selectedItems)) {
            final @NotNull List<String> current = new ArrayList<>(Objects.requireNonNullElse(values, List.<String>of()));
            // An empty list still needs one span, or there is nowhere to type.
            if (current.isEmpty()) current.add("");

            originalValues.add(new ArrayList<>(current));
            activeValues.add(new ArrayList<>(current));
        }

        editors = new BulkJsonEditors(p);
        editors.setOriginalTextSource(this::originalEscapedAt);
        render();

        components = List.of(ComponentDialogBase.of(editors));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Save", this::submit),
                StatusBarShortcut.build(Shortcuts.AddArrayItem, "Add", this::addItemAtCarets),
                StatusBarShortcut.build(Shortcuts.RemoveArrayItem, "Remove", this::removeItemAtCarets),
                StatusBarShortcut.build(Shortcuts.TabNext, "Next", () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.TabPrevious, "Previous", () -> editors.navigate(-1, true)),
                // The arrows wrap here, unlike the value dialog: an array item is
                // one line, so the caret has nowhere else to go.
                StatusBarShortcut.build(Shortcuts.ArrowDown, "Next", () -> editors.navigate(1, true)),
                StatusBarShortcut.build(Shortcuts.ArrowUp, "Previous", () -> editors.navigate(-1, true)),
                StatusBarShortcut.build(Shortcuts.CaretOnEveryValue, "All Carets", this::caretOnEveryValue),
                StatusBarShortcut.hint("Ctrl+Click", "Multi-Caret"),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        preferredSize = new Dimension(JBUI.scale(1100), JBUI.scale(550));

        // The editor listens to the action system, not to Swing key bindings.
        editors.bindKeysToEditor(shortcuts);
    }

    // ------------------------------------------------------------------
    // What a concrete section supplies.
    // ------------------------------------------------------------------

    protected abstract void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues);

    protected abstract @NotNull String getPopupTitle();

    protected abstract @NotNull String getArrayFieldName();

    protected abstract @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items);

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

    /**
     * Told where an editable item landed in the text being built.
     */
    @FunctionalInterface
    private interface ItemRecorder {
        void record(int start, int end, int testCaseIndex, int itemIndex);
    }

    /**
     * The read-only side: it is written the same way and remembers nothing.
     */
    private static final @NotNull ItemRecorder RECORDS_NOTHING = (start, end, testCaseIndex, itemIndex) -> {
    };

    @Override
    protected void submit() {
        readEditorIntoValues();
        applyValues(selectedItems, activeValues);
        // todo, apply update automation edit bulk test cases.
        updatedItems.accept(selectedItems);

        closeOk();
    }

    /**
     * Ctrl+Enter: a new empty item after each item under a caret.
     */
    private void addItemAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            activeValues.get(owner[0]).add(owner[1] + 1, "");
        }

        // The last span is the topmost one, because indicesUnderCarets orders
        // them last-first so the mutations above stay valid.
        final int[] focus = spanOwners.get(spans.getLast());
        render();
        focusItem(focus[0], focus[1] + 1);
    }

    /**
     * Shift+Delete: drops each item under a caret. The last item of a test case
     * is emptied rather than removed - a case with no items has nowhere to type.
     */
    private void removeItemAtCarets() {
        readEditorIntoValues();

        final @NotNull List<Integer> spans = editors.indicesUnderCarets();
        if (spans.isEmpty()) return;

        final int focusTc = spanOwners.get(spans.getLast())[0];
        int focusItem = 0;

        for (final int span : spans) {
            final int[] owner = spanOwners.get(span);
            final @NotNull List<String> items = activeValues.get(owner[0]);

            if (items.size() > 1) {
                items.remove(owner[1]);
                focusItem = Math.min(owner[1], items.size() - 1);
            } else {
                items.set(0, "");
                focusItem = 0;
            }
        }

        render();
        focusItem(focusTc, focusItem);
    }

    private void caretOnEveryValue() {
        readEditorIntoValues();
        editors.caretOnEveryValue();
    }

    /**
     * Takes what is on screen back into the values. Every gesture that rebuilds
     * the text starts here, or an edit made just before it would be discarded.
     */
    private void readEditorIntoValues() {
        for (int span = 0; span < spanOwners.size() && span < editors.valueCount(); span++) {
            final int[] owner = spanOwners.get(span);
            editors.valueAt(span).ifPresent(text ->
                    activeValues.get(owner[0]).set(owner[1], BulkJsonEditor.unescapeJson(text)));
        }
    }

    /**
     * Writes both sides from the current values, recording which item each
     * editable span belongs to.
     */
    private void render() {
        final @NotNull StringBuilder left = new StringBuilder("[\n");
        final @NotNull StringBuilder right = new StringBuilder("[\n");
        final @NotNull List<int[]> editableRanges = new ArrayList<>();

        spanOwners.clear();

        for (int i = 0; i < selectedItems.size(); i++) {
            final @NotNull TestCaseDto tc = selectedItems.get(i);
            final @NotNull String prefix = "  {\n    \"id\": \"" + BulkJsonEditor.escapeJson(tc.getId().toString())
                    + "\",\n    \"description\": \"" + BulkJsonEditor.escapeJson(tc.getDescription())
                    + "\",\n    \"" + getArrayFieldName() + "\": [\n";
            left.append(prefix);
            right.append(prefix);

            appendItems(left, originalValues.get(i), RECORDS_NOTHING, i);
            appendItems(right, activeValues.get(i), (start, end, testCase, item) -> {
                editableRanges.add(new int[]{start, end});
                spanOwners.add(new int[]{testCase, item});
            }, i);

            final @NotNull String suffix = "    ]\n  }";
            final @NotNull String comma = (i < selectedItems.size() - 1) ? ",\n" : "\n";
            left.append(suffix).append(comma);
            right.append(suffix).append(comma);
        }

        left.append("]");
        right.append("]");

        editors.setContent(left.toString(), right.toString(), editableRanges);
    }

    /**
     * One quoted item per line, telling the recorder where each one landed. The
     * left side records nothing, because nothing there is editable - which it
     * says with a recorder that does nothing rather than with two nulls (#71).
     */
    private void appendItems(final @NotNull StringBuilder out, final @NotNull List<String> items,
                             final @NotNull ItemRecorder recorder, final int testCaseIndex) {
        for (int j = 0; j < items.size(); j++) {
            out.append("      \"");

            final int start = out.length();
            out.append(BulkJsonEditor.escapeJson(items.get(j)));

            recorder.record(start, out.length(), testCaseIndex, j);

            out.append("\"").append(j < items.size() - 1 ? "," : "").append("\n");
        }
    }

    private void focusItem(final int testCaseIndex, final int itemIndex) {
        for (int span = 0; span < spanOwners.size(); span++) {
            final int[] owner = spanOwners.get(span);
            if (owner[0] == testCaseIndex && owner[1] == itemIndex) {
                editors.focusValue(span);
                return;
            }
        }
    }

    /**
     * What the span at this index started as, escaped the way the editor shows
     * it. Spans added since the dialog opened have no original, so they always
     * read as changed.
     */
    private @NotNull String originalEscapedAt(final int span) {
        if (span >= spanOwners.size()) return "";

        final int[] owner = spanOwners.get(span);
        final @NotNull List<String> original = originalValues.get(owner[0]);

        return owner[1] < original.size() ? BulkJsonEditor.escapeJson(original.get(owner[1])) : "";
    }
}
