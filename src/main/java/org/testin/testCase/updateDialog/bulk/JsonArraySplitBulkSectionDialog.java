package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class JsonArraySplitBulkSectionDialog {
    protected final @NotNull Project p;

    public JsonArraySplitBulkSectionDialog(final @NotNull Project p) {
        this.p = p;
    }

    protected abstract void applyValues(final @NotNull List<TestCaseDto> items, final @NotNull List<List<String>> newValues);

    protected abstract @NotNull String getPopupTitle();

    protected abstract @NotNull String getArrayFieldName();

    protected abstract @NotNull List<List<String>> extractOriginalValues(final @NotNull List<TestCaseDto> items);

    public void show(final @NotNull List<TestCaseDto> selectedItems, final @Nullable Consumer<List<TestCaseDto>> updatedItems) {
        final List<List<String>> originalValues = new ArrayList<>();
        final List<List<String>> activeValues = new ArrayList<>();

        final List<List<String>> extracted = extractOriginalValues(selectedItems);
        for (final List<String> list : extracted) {
            final List<String> current = list != null ? new ArrayList<>(list) : new ArrayList<>();
            if (current.isEmpty()) current.add("");
            originalValues.add(new ArrayList<>(current));
            activeValues.add(new ArrayList<>(current));
        }

        final Document leftDoc = EditorFactory.getInstance().createDocument("");
        final Editor leftEditor = EditorFactory.getInstance().createViewer(leftDoc, p);
        setupEditorAppearance(leftEditor, p);
        leftEditor.getContentComponent().setFocusable(false);
        leftEditor.getSettings().setCaretRowShown(false);
        leftEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                event.consume();
            }
        });

        final Document rightDoc = EditorFactory.getInstance().createDocument("");
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(rightDoc, e -> {
        });
        final Editor rightEditor = EditorFactory.getInstance().createEditor(rightDoc, p);
        setupEditorAppearance(rightEditor, p);

        final List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();
        final List<ItemMarker> itemMarkers = new ArrayList<>();
        final List<RangeMarker> guardBlocks = new ArrayList<>();

        Color themeCaretRowColor = rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (themeCaretRowColor == null) themeCaretRowColor = new JBColor(Gray._245, Gray._50);
        final TextAttributes leftLineAttr = new TextAttributes();
        leftLineAttr.setBackgroundColor(themeCaretRowColor);

        final Runnable syncStateFromEditor = () -> {
            for (final ItemMarker sm : itemMarkers) {
                if (sm.marker != null && sm.marker.isValid()) {
                    final String text = rightDoc.getText(new TextRange(sm.marker.getStartOffset(), sm.marker.getEndOffset()));
                    activeValues.get(sm.tcIdx).set(sm.itemIdx, unescapeJson(text));
                }
            }
        };

        final BiConsumer<Integer, Integer> renderUI = (focusTc, focusItem) -> {
            final StringBuilder leftSb = new StringBuilder("[\n");
            final StringBuilder rightSb = new StringBuilder("[\n");
            final List<ItemMarker> tempMarkers = new ArrayList<>();

            for (int i = 0; i < selectedItems.size(); i++) {
                final TestCaseDto tc = selectedItems.get(i);
                final String id = escapeJson(tc.getId().toString());
                final String escapedDescription = escapeJson(tc.getDescription());

                final String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"description\": \"" + escapedDescription + "\",\n    \"" + getArrayFieldName() + "\": [\n";
                leftSb.append(prefix);
                rightSb.append(prefix);

                final List<String> origItems = originalValues.get(i);
                for (int j = 0; j < origItems.size(); j++) {
                    final String itemPrefix = "      \"";
                    final String itemSuffix = "\"" + (j < origItems.size() - 1 ? "," : "") + "\n";
                    leftSb.append(itemPrefix).append(escapeJson(origItems.get(j))).append(itemSuffix);
                }

                final List<String> currItems = activeValues.get(i);
                for (int j = 0; j < currItems.size(); j++) {
                    final String itemPrefix = "      \"";
                    final String itemSuffix = "\"" + (j < currItems.size() - 1 ? "," : "") + "\n";

                    rightSb.append(itemPrefix);

                    final ItemMarker sm = new ItemMarker();
                    sm.tcIdx = i;
                    sm.itemIdx = j;
                    sm.startOffset = rightSb.length();
                    rightSb.append(escapeJson(currItems.get(j)));
                    sm.endOffset = rightSb.length();
                    tempMarkers.add(sm);

                    rightSb.append(itemSuffix);
                }

                final String suffix = "    ]\n  }";
                final String comma = (i < selectedItems.size() - 1) ? ",\n" : "\n";
                leftSb.append(suffix).append(comma);
                rightSb.append(suffix).append(comma);
            }
            leftSb.append("]");
            rightSb.append("]");

            WriteCommandAction.runWriteCommandAction(p, () -> {
                for (final RangeMarker g : guardBlocks) rightDoc.removeGuardedBlock(g);
                guardBlocks.clear();
                itemMarkers.clear();

                leftDoc.setReadOnly(false);
                leftDoc.setText(leftSb.toString());
                leftDoc.setReadOnly(true);

                rightDoc.setText(rightSb.toString());

                int currentGuardStart = 0;
                for (final ItemMarker sm : tempMarkers) {
                    final RangeMarker rm = rightDoc.createRangeMarker(sm.startOffset, sm.endOffset);
                    rm.setGreedyToLeft(true);
                    rm.setGreedyToRight(true);
                    sm.marker = rm;
                    itemMarkers.add(sm);

                    if (currentGuardStart < sm.startOffset) {
                        guardBlocks.add(rightDoc.createGuardedBlock(currentGuardStart, sm.startOffset));
                    }
                    currentGuardStart = sm.endOffset;
                }
                if (currentGuardStart < rightDoc.getTextLength()) {
                    guardBlocks.add(rightDoc.createGuardedBlock(currentGuardStart, rightDoc.getTextLength()));
                }
            });

            if (focusTc != null && focusItem != null) {
                for (final ItemMarker sm : itemMarkers) {
                    if (sm.tcIdx == focusTc && sm.itemIdx == focusItem && sm.marker != null) {
                        rightEditor.getCaretModel().moveToOffset(sm.marker.getEndOffset());
                        break;
                    }
                }
            }
        };

        final Runnable updateRowHighlights = () -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;
            final MarkupModel leftMarkup = leftEditor.getMarkupModel();
            for (final RangeHighlighter h : leftLineHighlighters) leftMarkup.removeHighlighter(h);
            leftLineHighlighters.clear();

            for (final Caret caret : rightEditor.getCaretModel().getAllCarets()) {
                final int line = rightDoc.getLineNumber(caret.getOffset());
                if (line < leftDoc.getLineCount()) {
                    leftLineHighlighters.add(leftMarkup.addLineHighlighter(line, HighlighterLayer.CARET_ROW, leftLineAttr));
                }
            }
        };

        rightEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(final @NotNull CaretEvent event) {
                final Caret caret = event.getCaret();
                final int offset = caret.getOffset();
                final boolean isInsideEditable = itemMarkers.stream().anyMatch(m ->
                        m.marker != null && m.marker.isValid() && offset >= m.marker.getStartOffset() && offset <= m.marker.getEndOffset());
                if (!isInsideEditable) caret.moveToOffset(getNearestValidOffset(offset, itemMarkers));
                updateRowHighlights.run();
            }

            @Override
            public void caretAdded(final @NotNull CaretEvent event) {
                updateRowHighlights.run();
            }

            @Override
            public void caretRemoved(final @NotNull CaretEvent event) {
                updateRowHighlights.run();
            }
        });

        final Disposable docListenerDisposable = Disposer.newDisposable();
        rightDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (rightEditor.isDisposed()) return;
                    final MarkupModel markupModel = rightEditor.getMarkupModel();
                    for (final RangeHighlighter h : markupModel.getAllHighlighters()) {
                        if (h.getLayer() == HighlighterLayer.SELECTION - 1) markupModel.removeHighlighter(h);
                    }
                    final TextAttributes diffAttr = new TextAttributes();
                    diffAttr.setBackgroundColor(new JBColor(new Color(228, 250, 228), new Color(43, 61, 44)));

                    for (final ItemMarker sm : itemMarkers) {
                        if (sm.marker != null && sm.marker.isValid()) {
                            final String currentText = rightDoc.getText(new TextRange(sm.marker.getStartOffset(), sm.marker.getEndOffset()));
                            final List<String> origList = originalValues.get(sm.tcIdx);
                            final String originalText = (sm.itemIdx < origList.size()) ? escapeJson(origList.get(sm.itemIdx)) : "";

                            if (!currentText.equals(originalText)) {
                                markupModel.addRangeHighlighter(sm.marker.getStartOffset(), sm.marker.getEndOffset(),
                                        HighlighterLayer.SELECTION - 1, diffAttr, HighlighterTargetArea.EXACT_RANGE);
                            }
                        }
                    }
                });
            }
        }, docListenerDisposable);

        rightEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(final @NotNull EditorMouseEvent event) {
                final MouseEvent e = event.getMouseEvent();
                if (e.isControlDown() || e.isMetaDown()) {
                    VisualPosition visualPos = rightEditor.xyToVisualPosition(e.getPoint());
                    int offset = rightEditor.logicalPositionToOffset(rightEditor.visualToLogicalPosition(visualPos));
                    final int finalOffset = offset;
                    final boolean isInsideEditable = itemMarkers.stream().anyMatch(m ->
                            m.marker != null && m.marker.isValid() && finalOffset >= m.marker.getStartOffset() && finalOffset <= m.marker.getEndOffset());

                    if (!isInsideEditable) {
                        offset = getNearestValidOffset(offset, itemMarkers);
                        visualPos = rightEditor.logicalToVisualPosition(rightEditor.offsetToLogicalPosition(offset));
                    }
                    final CaretModel caretModel = rightEditor.getCaretModel();
                    final Caret existingCaret = caretModel.getCaretAt(visualPos);
                    if (existingCaret != null) {
                        if (caretModel.getCaretCount() > 1) caretModel.removeCaret(existingCaret);
                    } else {
                        caretModel.addCaret(visualPos, true);
                    }
                    event.consume();
                }
            }
        });

        leftEditor.getScrollingModel().addVisibleAreaListener(e -> rightEditor.getScrollingModel().scrollVertically(e.getNewRectangle().y));
        rightEditor.getScrollingModel().addVisibleAreaListener(e -> leftEditor.getScrollingModel().scrollVertically(e.getNewRectangle().y));

        final JBSplitter splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(leftEditor.getComponent());
        splitter.setSecondComponent(rightEditor.getComponent());

        final JBPanel<?> statusBar = new JBPanel<>(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        final JBLabel shortcutLabel = new JBLabel("💡 Shortcuts:  [Enter] Save  |  [Ctrl+Enter] Add  |  [Shift+Del] Remove  |  [Ctrl+Click] Multi-Caret  |  [Ctrl+Shift+A] All Carets");
        shortcutLabel.setForeground(JBColor.GRAY);
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        statusBar.add(shortcutLabel, BorderLayout.WEST);

        final JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        DialogStyle.styleContent(panel);
        panel.add(splitter, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(JBUI.scale(1100), JBUI.scale(550)));

        final JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, rightEditor.getContentComponent())
                .setTitle(getPopupTitle())
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setCancelOnWindowDeactivation(false)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        final Runnable saveLogic = () -> {
            syncStateFromEditor.run();
            applyValues(selectedItems, activeValues);
            if (updatedItems != null)
                // todo, apply update automation edit bulk test cases. set to null for now
                updatedItems.accept(selectedItems);
            popup.closeOk(null);
        };

        final JComponent keymapTarget = rightEditor.getContentComponent();
        KeyAction.register(saveLogic, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), keymapTarget);
        KeyAction.register(saveLogic, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), keymapTarget);

        KeyAction.register(() -> {
            syncStateFromEditor.run();
            final List<int[]> targets = collectCaretTargets(rightEditor, itemMarkers);
            if (targets.isEmpty()) return;

            for (final int[] target : targets) {
                activeValues.get(target[0]).add(target[1] + 1, "");
            }

            final int[] firstAdded = targets.getLast();
            renderUI.accept(firstAdded[0], firstAdded[1] + 1);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), keymapTarget);

        KeyAction.register(() -> {
            syncStateFromEditor.run();
            final List<int[]> targets = collectCaretTargets(rightEditor, itemMarkers);
            if (targets.isEmpty()) return;

            final int focusTc = targets.getLast()[0];
            int focusStep = 0;

            for (final int[] target : targets) {
                final List<String> itemsList = activeValues.get(target[0]);
                if (itemsList.size() > 1) {
                    itemsList.remove(target[1]);
                    focusStep = Math.min(target[1], itemsList.size() - 1);
                } else {
                    itemsList.set(0, "");
                    focusStep = 0;
                }
            }
            renderUI.accept(focusTc, focusStep);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_DOWN_MASK), keymapTarget);

        KeyAction.register(() -> navigate(1, rightEditor, itemMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), keymapTarget);
        KeyAction.register(() -> navigate(1, rightEditor, itemMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), keymapTarget);
        KeyAction.register(() -> navigate(-1, rightEditor, itemMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), keymapTarget);
        KeyAction.register(() -> navigate(-1, rightEditor, itemMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), keymapTarget);

        KeyAction.register(() -> {
            syncStateFromEditor.run();
            placeCaretOnAllItems(rightEditor, itemMarkers);
            updateRowHighlights.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK), keymapTarget);

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                Disposer.dispose(docListenerDisposable);
                if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
                if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
            }
        });

        renderUI.accept(0, 0);
        popup.showCenteredInCurrentWindow(p);
    }

    /**
     * The (test case, item) pairs currently under carets, deduplicated and sorted
     * bottom-up so index-shifting mutations stay valid while applying.
     */
    private @NotNull List<int[]> collectCaretTargets(final @NotNull Editor editor, final @NotNull List<ItemMarker> markers) {
        final List<int[]> targets = new ArrayList<>();

        for (final Caret caret : editor.getCaretModel().getAllCarets()) {
            final int offset = caret.getOffset();
            final ItemMarker current = markers.stream()
                    .filter(m -> m.marker != null && offset >= m.marker.getStartOffset() && offset <= m.marker.getEndOffset())
                    .findFirst().orElse(null);
            if (current != null && targets.stream().noneMatch(arr -> arr[0] == current.tcIdx && arr[1] == current.itemIdx)) {
                targets.add(new int[]{current.tcIdx, current.itemIdx});
            }
        }

        targets.sort((a, b) -> {
            if (a[0] != b[0]) return Integer.compare(b[0], a[0]);
            return Integer.compare(b[1], a[1]);
        });
        return targets;
    }

    /**
     * Ctrl+Shift+A: one caret at the end of every editable item.
     */
    private void placeCaretOnAllItems(final @NotNull Editor editor, final @NotNull List<ItemMarker> markers) {
        final CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final ItemMarker sm : markers) {
            if (sm.marker == null || !sm.marker.isValid()) continue;

            final LogicalPosition logPos = editor.offsetToLogicalPosition(sm.marker.getEndOffset());
            final VisualPosition visPos = editor.logicalToVisualPosition(logPos);

            if (isFirst) {
                caretModel.moveToVisualPosition(visPos);
                isFirst = false;
            } else {
                caretModel.addCaret(visPos, true);
            }
        }
    }

    private void navigate(final int direction, final @NotNull Editor editor, final @NotNull List<ItemMarker> markers) {
        if (markers.isEmpty()) return;

        editor.getCaretModel().removeSecondaryCarets();
        final int offset = editor.getCaretModel().getOffset();
        int currentIndex = 0;
        for (int i = 0; i < markers.size(); i++) {
            // Use the live marker offsets: the snapshotted startOffset/endOffset
            // go stale as soon as the user types, sending the caret to the wrong item.
            final RangeMarker marker = markers.get(i).marker;
            if (marker != null && marker.isValid()
                    && offset >= marker.getStartOffset() && offset <= marker.getEndOffset()) {
                currentIndex = i;
                break;
            }
        }
        final int targetIndex = (currentIndex + direction + markers.size()) % markers.size();
        final RangeMarker target = markers.get(targetIndex).marker;
        if (target != null && target.isValid()) {
            editor.getCaretModel().moveToOffset(target.getEndOffset());
        }
    }

    private int getNearestValidOffset(final int offset, final @NotNull List<ItemMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (final ItemMarker m : markers) {
            if (m.marker == null || !m.marker.isValid()) continue;
            if (Math.abs(offset - m.marker.getStartOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.marker.getStartOffset());
                nearestOffset = m.marker.getStartOffset();
            }
            if (Math.abs(offset - m.marker.getEndOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.marker.getEndOffset());
                nearestOffset = m.marker.getEndOffset();
            }
        }
        return nearestOffset;
    }

    private void setupEditorAppearance(final @NotNull Editor editor, final @NotNull Project p) {
        final FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        final com.intellij.openapi.editor.highlighter.EditorHighlighter highlighter = com.intellij.openapi.editor.highlighter.EditorHighlighterFactory.getInstance().createEditorHighlighter(p, jsonFileType);
        if (editor instanceof EditorEx) ((EditorEx) editor).setHighlighter(highlighter);
        final EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15f);
        scheme.setLineSpacing(1.4f);
        final EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(true);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(1);
    }

    protected @NotNull String escapeJson(final @Nullable String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    protected @NotNull String unescapeJson(final @Nullable String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }

}
