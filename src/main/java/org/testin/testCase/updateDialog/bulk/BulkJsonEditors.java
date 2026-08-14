package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.framework.IDialogComponent;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * The bulk editor pair: the original JSON on the left, read-only, and an
 * editable copy on the right where only the values are writable.
 * <p>
 * A framework dialog component, so the bulk dialogs declare it as content
 * instead of assembling a popup themselves. It knows about editable ranges and
 * nothing else - a value per test case or an item of an array per test case
 * look identical from here, which is why one component serves both dialogs.
 */
final class BulkJsonEditors implements IDialogComponent {

    private final @NotNull Project p;

    private final @NotNull Document leftDoc;
    private final @NotNull Document rightDoc;
    private final @NotNull Editor leftEditor;
    private final @NotNull Editor rightEditor;
    private final @NotNull JBSplitter splitter;

    private final @NotNull List<RangeMarker> markers = new ArrayList<>();
    private final @NotNull List<RangeMarker> guardBlocks = new ArrayList<>();
    private final @NotNull List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();
    private final @NotNull TextAttributes leftLineAttr = new TextAttributes();
    private final @NotNull Disposable docListenerDisposable = Disposer.newDisposable();

    /**
     * The escaped text each value started as, by marker index. Diff highlighting
     * compares against it; the dialogs know what "original" means, this does not.
     */
    private @Nullable IntFunction<String> originalTextAt;

    BulkJsonEditors(final @NotNull Project p) {
        this.p = p;

        leftDoc = EditorFactory.getInstance().createDocument("");
        leftEditor = EditorFactory.getInstance().createViewer(leftDoc, p);
        BulkJsonEditor.setupEditorAppearance(leftEditor, p);
        leftEditor.getContentComponent().setFocusable(false);
        leftEditor.getSettings().setCaretRowShown(false);
        // The left side is context, not a place to put a caret.
        leftEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                event.consume();
            }
        });

        rightDoc = EditorFactory.getInstance().createDocument("");
        // Silences the platform's "cannot modify" popup when a keystroke lands
        // on a guarded block; the guard still refuses the edit.
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(rightDoc, e -> {
        });
        rightEditor = EditorFactory.getInstance().createEditor(rightDoc, p);
        BulkJsonEditor.setupEditorAppearance(rightEditor, p);

        Color caretRowColor = rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (caretRowColor == null) caretRowColor = new JBColor(Gray._245, Gray._50);
        leftLineAttr.setBackgroundColor(caretRowColor);

        splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(leftEditor.getComponent());
        splitter.setSecondComponent(rightEditor.getComponent());

        installCaretSnapping();
        installDiffHighlighting();
        installScrollSync();
        installMultiCaretClick();
    }

    /**
     * Replaces both sides. The editable ranges are offsets into the right text;
     * everything between and around them becomes a guarded block, so the JSON
     * shape cannot be typed over.
     * <p>
     * Called once by the value dialog and again on every add or remove by the
     * array dialog, which is why the previous guards are torn down first.
     */
    void setContent(final @NotNull String leftText, final @NotNull String rightText,
                    final @NotNull List<int[]> editableRanges) {
        WriteCommandAction.runWriteCommandAction(p, () -> {
            for (final RangeMarker guard : guardBlocks) rightDoc.removeGuardedBlock(guard);
            guardBlocks.clear();
            markers.clear();

            leftDoc.setReadOnly(false);
            leftDoc.setText(leftText);
            leftDoc.setReadOnly(true);

            rightDoc.setText(rightText);

            int guardStart = 0;
            for (final int[] range : editableRanges) {
                final RangeMarker marker = rightDoc.createRangeMarker(range[0], range[1]);
                marker.setGreedyToLeft(true);
                marker.setGreedyToRight(true);
                markers.add(marker);

                if (guardStart < range[0]) guardBlocks.add(rightDoc.createGuardedBlock(guardStart, range[0]));
                guardStart = range[1];
            }
            if (guardStart < rightDoc.getTextLength()) {
                guardBlocks.add(rightDoc.createGuardedBlock(guardStart, rightDoc.getTextLength()));
            }
        });
    }

    /**
     * What each value started as, by marker index, so an untouched value can be
     * told from an edited one.
     */
    void setOriginalTextSource(final @NotNull IntFunction<String> originalTextAt) {
        this.originalTextAt = originalTextAt;
    }

    int valueCount() {
        return markers.size();
    }

    /**
     * The current text of one value, or null when its marker did not survive an
     * edit - the caller decides what an unreadable value means.
     */
    @Nullable String valueAt(final int index) {
        final RangeMarker marker = markers.get(index);
        if (!marker.isValid()) return null;

        return rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
    }

    /**
     * The value indices under carets, deduplicated and ordered last-first so a
     * caller mutating a list by index stays valid as it goes.
     */
    @NotNull List<Integer> indicesUnderCarets() {
        final List<Integer> indices = new ArrayList<>();

        for (final Caret caret : rightEditor.getCaretModel().getAllCarets()) {
            final int index = indexAt(caret.getOffset());
            if (index >= 0 && !indices.contains(index)) indices.add(index);
        }

        indices.sort((a, b) -> Integer.compare(b, a));
        return indices;
    }

    /**
     * Puts the caret at the end of one value, which is where typing continues.
     */
    void focusValue(final int index) {
        if (index < 0 || index >= markers.size()) return;

        final RangeMarker marker = markers.get(index);
        if (marker.isValid()) rightEditor.getCaretModel().moveToOffset(marker.getEndOffset());
    }

    /**
     * Moves to the neighboring value. Reads the marker offsets live, because a
     * position captured when the text was built goes stale as soon as anyone types.
     */
    void navigate(final int direction, final boolean wrap) {
        if (markers.isEmpty()) return;

        rightEditor.getCaretModel().removeSecondaryCarets();

        final int current = Math.max(0, indexAt(rightEditor.getCaretModel().getOffset()));
        final int target = wrap
                ? (current + direction + markers.size()) % markers.size()
                : current + direction;
        if (target < 0 || target >= markers.size()) return;

        focusValue(target);
    }

    /**
     * Ctrl+Shift+A: one caret at the end of every value.
     */
    void caretOnEveryValue() {
        BulkJsonEditor.placeCaretOnAll(rightEditor, liveMarkers());
        refreshRowHighlights();
    }

    /**
     * Releases both editors. The platform does not reclaim them with the popup,
     * so the dialog calls this when it closes.
     */
    void release() {
        Disposer.dispose(docListenerDisposable);
        if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
        if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
    }

    // ------------------------------------------------------------------
    // Behavior installed once, in the constructor.
    // ------------------------------------------------------------------

    /**
     * A caret that lands on the JSON around a value is pulled to the nearest
     * place it can actually type.
     */
    private void installCaretSnapping() {
        rightEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(final @NotNull CaretEvent event) {
                final Caret caret = event.getCaret();
                if (indexAt(caret.getOffset()) < 0) {
                    caret.moveToOffset(BulkJsonEditor.nearestValidOffset(caret.getOffset(), liveMarkers()));
                }
                refreshRowHighlights();
            }

            @Override
            public void caretAdded(final @NotNull CaretEvent event) {
                refreshRowHighlights();
            }

            @Override
            public void caretRemoved(final @NotNull CaretEvent event) {
                refreshRowHighlights();
            }
        });
    }

    /**
     * Values that differ from what they started as get a green background, so an
     * edit is visible without comparing the two sides by eye.
     */
    private void installDiffHighlighting() {
        rightDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(BulkJsonEditors.this::refreshDiffHighlights);
            }
        }, docListenerDisposable);
    }

    private void refreshDiffHighlights() {
        if (rightEditor.isDisposed() || originalTextAt == null) return;

        final MarkupModel markup = rightEditor.getMarkupModel();
        for (final RangeHighlighter highlighter : markup.getAllHighlighters()) {
            if (highlighter.getLayer() == HighlighterLayer.SELECTION - 1) markup.removeHighlighter(highlighter);
        }

        final TextAttributes diffAttr = new TextAttributes();
        diffAttr.setBackgroundColor(new JBColor(new Color(228, 250, 228), new Color(43, 61, 44)));

        for (int i = 0; i < markers.size(); i++) {
            final String current = valueAt(i);
            if (current == null || current.equals(originalTextAt.apply(i))) continue;

            final RangeMarker marker = markers.get(i);
            markup.addRangeHighlighter(marker.getStartOffset(), marker.getEndOffset(),
                    HighlighterLayer.SELECTION - 1, diffAttr, HighlighterTargetArea.EXACT_RANGE);
        }
    }

    /**
     * The two sides scroll as one, so a value always faces what it started as.
     */
    private void installScrollSync() {
        leftEditor.getScrollingModel().addVisibleAreaListener(e -> {
            final int target = e.getNewRectangle().y;
            if (rightEditor.getScrollingModel().getVerticalScrollOffset() != target) {
                rightEditor.getScrollingModel().scrollVertically(target);
            }
        });
        rightEditor.getScrollingModel().addVisibleAreaListener(e -> {
            final int target = e.getNewRectangle().y;
            if (leftEditor.getScrollingModel().getVerticalScrollOffset() != target) {
                leftEditor.getScrollingModel().scrollVertically(target);
            }
        });
    }

    /**
     * Ctrl+Click adds or removes a caret, so the same edit can be typed into
     * several values at once.
     */
    private void installMultiCaretClick() {
        rightEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                final MouseEvent mouse = event.getMouseEvent();
                if (!mouse.isControlDown() && !mouse.isMetaDown()) return;

                VisualPosition position = rightEditor.xyToVisualPosition(mouse.getPoint());
                final int offset = rightEditor.logicalPositionToOffset(rightEditor.visualToLogicalPosition(position));

                if (indexAt(offset) < 0) {
                    final int snapped = BulkJsonEditor.nearestValidOffset(offset, liveMarkers());
                    position = rightEditor.logicalToVisualPosition(rightEditor.offsetToLogicalPosition(snapped));
                }

                final CaretModel caretModel = rightEditor.getCaretModel();
                final Caret existing = caretModel.getCaretAt(position);
                if (existing != null) {
                    if (caretModel.getCaretCount() > 1) caretModel.removeCaret(existing);
                } else {
                    caretModel.addCaret(position, true);
                }
                event.consume();
            }
        });
    }

    /**
     * Highlights the left-hand line facing each caret, so the pair reads as rows.
     */
    private void refreshRowHighlights() {
        if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;

        final MarkupModel leftMarkup = leftEditor.getMarkupModel();
        for (final RangeHighlighter highlighter : leftLineHighlighters) leftMarkup.removeHighlighter(highlighter);
        leftLineHighlighters.clear();

        for (final Caret caret : rightEditor.getCaretModel().getAllCarets()) {
            final int line = rightDoc.getLineNumber(caret.getOffset());
            if (line < leftDoc.getLineCount()) {
                leftLineHighlighters.add(leftMarkup.addLineHighlighter(line, HighlighterLayer.CARET_ROW, leftLineAttr));
            }
        }
    }

    /**
     * The value containing this offset, or -1 when the offset is in the JSON
     * around the values.
     */
    private int indexAt(final int offset) {
        for (int i = 0; i < markers.size(); i++) {
            final RangeMarker marker = markers.get(i);
            if (marker.isValid() && offset >= marker.getStartOffset() && offset <= marker.getEndOffset()) return i;
        }
        return -1;
    }

    private @NotNull List<RangeMarker> liveMarkers() {
        return markers.stream().filter(RangeMarker::isValid).toList();
    }

    // ------------------------------------------------------------------
    // IDialogComponent.
    // ------------------------------------------------------------------

    @Override
    public @NotNull JComponent getPanel() {
        return splitter;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return rightEditor.getContentComponent();
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // Nothing here submits on its own; Enter is a declared shortcut.
    }

    @Override
    public boolean fillsSpace() {
        return true;
    }

    /**
     * False because a Swing key binding on an editor is not reached: an IntelliJ
     * editor consumes keys through its own action handlers first, so Enter
     * inserted a newline instead of saving. {@link #bindKeysToEditor} registers
     * the same declaration through the action system instead.
     */
    @Override
    public boolean acceptsDialogKeys() {
        return false;
    }

    /**
     * Binds the dialog's declared shortcuts on the editor through the action
     * system, which is what an editor listens to. The declaration is still the
     * one source - the status bar renders from it and this binds from it.
     */
    void bindKeysToEditor(final @NotNull List<StatusBarShortcut> shortcuts) {
        final JComponent target = rightEditor.getContentComponent();

        for (final StatusBarShortcut shortcut : shortcuts) {
            if (!shortcut.isBindable()) continue;

            final Shortcuts key = Objects.requireNonNull(shortcut.shortcut());
            final Runnable action = Objects.requireNonNull(shortcut.action());
            register(action, key.getKey(), target);

            // Shift+Enter saves as well. It is not advertised: it exists so the
            // gesture that normally inserts a line break cannot put a newline
            // inside a value the JSON shape says is one line.
            if (key == Shortcuts.Enter) {
                register(action, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), target);
            }
        }
    }

    private static void register(final @NotNull Runnable body, final @NotNull KeyStroke keyStroke,
                                 final @NotNull JComponent target) {
        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                body.run();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), target);
    }

    /**
     * Called once the dialog is on screen: the first value takes the caret, and
     * the row highlight follows it.
     */
    void focusFirstValue() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;

            focusValue(0);
            refreshRowHighlights();
            refreshDiffHighlights();
        });
    }
}
