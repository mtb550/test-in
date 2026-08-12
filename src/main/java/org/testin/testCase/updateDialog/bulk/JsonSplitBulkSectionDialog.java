package org.testin.testCase.updateDialog.bulk;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
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
import org.testin.mappers.dto.TestCaseDto;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class JsonSplitBulkSectionDialog {
    protected final @NotNull Project p;

    public JsonSplitBulkSectionDialog(final @NotNull Project p) {
        this.p = p;
    }

    protected abstract String getPopupTitle();

    /**
     * JSON key of the edited field, e.g. "testData".
     */
    protected abstract String getJsonFieldName();

    protected abstract String getOriginalValue(final TestCaseDto tc);

    /**
     * Applies one edited (non-null, trimmed) value to the test case.
     */
    protected abstract void setValue(final TestCaseDto tc, final String value);

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

    protected void applyValues(final List<TestCaseDto> items, final List<String> newValues) {
        for (int i = 0; i < items.size(); i++) {
            final String raw = newValues.get(i);
            if (raw == null) continue; // unchanged row - never rewrite (see saveLogic)

            final String value = raw.trim();
            if (value.isEmpty() && !acceptsBlank()) continue;

            setValue(items.get(i), value);
        }
    }

    protected void appendJsonItem(final TestCaseDto tc, final int index, final boolean isLast, final StringBuilder leftSb, final StringBuilder rightSb, final List<int[]> rightEditableRanges) {
        final String escapedValue = escapeJson(getOriginalValue(tc));

        final StringBuilder prefixSb = new StringBuilder("  {\n    \"id\": \"")
                .append(escapeJson(tc.getId().toString())).append("\",\n");
        if (showsDescriptionContext()) {
            prefixSb.append("    \"description\": \"").append(escapeJson(tc.getDescription())).append("\",\n");
        }
        prefixSb.append("    \"").append(getJsonFieldName()).append("\": \"");

        final String prefix = prefixSb.toString();
        final String suffix = "\"\n  }";
        final String comma = isLast ? "\n" : ",\n";

        leftSb.append(prefix).append(escapedValue).append(suffix).append(comma);

        rightSb.append(prefix);
        final int startOffset = rightSb.length();
        rightSb.append(escapedValue);
        final int endOffset = rightSb.length();
        rightEditableRanges.add(new int[]{startOffset, endOffset});
        rightSb.append(suffix).append(comma);
    }

    public void show(final List<TestCaseDto> selectedItems, final Consumer<List<TestCaseDto>> updatedItems) {
        StringBuilder leftSb = new StringBuilder();
        StringBuilder rightSb = new StringBuilder();
        List<int[]> rightEditableRanges = new ArrayList<>();

        leftSb.append("[\n");
        rightSb.append("[\n");

        for (int i = 0; i < selectedItems.size(); i++) {
            boolean isLast = (i == selectedItems.size() - 1);
            appendJsonItem(selectedItems.get(i), i, isLast, leftSb, rightSb, rightEditableRanges);
        }

        leftSb.append("]");
        rightSb.append("]");

        Document leftDoc = EditorFactory.getInstance().createDocument(leftSb.toString());
        leftDoc.setReadOnly(true);
        Editor leftEditor = EditorFactory.getInstance().createViewer(leftDoc, p);
        setupEditorAppearance(leftEditor, p);

        leftEditor.getContentComponent().setFocusable(false);
        leftEditor.getSettings().setCaretRowShown(false);
        leftEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(@NotNull EditorMouseEvent event) {
                event.consume();
            }
        });

        Document rightDoc = EditorFactory.getInstance().createDocument(rightSb.toString());
        List<RangeMarker> valueMarkers = new ArrayList<>();

        int currentGuardStart = 0;
        for (int[] range : rightEditableRanges) {
            RangeMarker marker = rightDoc.createRangeMarker(range[0], range[1]);
            marker.setGreedyToLeft(true);
            marker.setGreedyToRight(true);
            valueMarkers.add(marker);

            if (currentGuardStart < range[0]) {
                rightDoc.createGuardedBlock(currentGuardStart, range[0]);
            }
            currentGuardStart = range[1];
        }
        if (currentGuardStart < rightDoc.getTextLength()) {
            rightDoc.createGuardedBlock(currentGuardStart, rightDoc.getTextLength());
        }

        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(rightDoc, e -> {
        });

        Editor rightEditor = EditorFactory.getInstance().createEditor(rightDoc, p);
        setupEditorAppearance(rightEditor, p);

        List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();
        Color themeCaretRowColor = rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (themeCaretRowColor == null) {
            themeCaretRowColor = new JBColor(Gray._245, Gray._50);
        }
        TextAttributes leftLineAttr = new TextAttributes();
        leftLineAttr.setBackgroundColor(themeCaretRowColor);

        Runnable updateRowHighlights = () -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;
            MarkupModel leftMarkup = leftEditor.getMarkupModel();
            for (RangeHighlighter h : leftLineHighlighters) leftMarkup.removeHighlighter(h);
            leftLineHighlighters.clear();

            for (Caret caret : rightEditor.getCaretModel().getAllCarets()) {
                int line = rightDoc.getLineNumber(caret.getOffset());
                if (line < leftDoc.getLineCount()) {
                    leftLineHighlighters.add(leftMarkup.addLineHighlighter(
                            line, HighlighterLayer.CARET_ROW, leftLineAttr
                    ));
                }
            }
        };

        rightEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                Caret caret = event.getCaret();

                int offset = caret.getOffset();
                boolean isInsideEditable = false;

                for (RangeMarker m : valueMarkers) {
                    if (offset >= m.getStartOffset() && offset <= m.getEndOffset()) {
                        isInsideEditable = true;
                        break;
                    }
                }

                if (!isInsideEditable) {
                    int nearestOffset = getNearestValidOffset(offset, valueMarkers);
                    caret.moveToOffset(nearestOffset);
                }
                updateRowHighlights.run();
            }

            @Override
            public void caretAdded(@NotNull CaretEvent event) {
                updateRowHighlights.run();
            }

            @Override
            public void caretRemoved(@NotNull CaretEvent event) {
                updateRowHighlights.run();
            }
        });

        Disposable docListenerDisposable = Disposer.newDisposable();
        MarkupModel rightMarkupModel = rightEditor.getMarkupModel();
        rightDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (rightEditor.isDisposed()) return;
                    for (RangeHighlighter h : rightMarkupModel.getAllHighlighters()) {
                        if (h.getLayer() == HighlighterLayer.SELECTION - 1) rightMarkupModel.removeHighlighter(h);
                    }

                    TextAttributes diffAttr = new TextAttributes();
                    diffAttr.setBackgroundColor(new JBColor(new Color(228, 250, 228), new Color(43, 61, 44)));

                    for (int i = 0; i < selectedItems.size(); i++) {
                        RangeMarker marker = valueMarkers.get(i);
                        if (marker.isValid()) {
                            String currentText = rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
                            String originalText = escapeJson(getOriginalValue(selectedItems.get(i)));

                            if (!currentText.equals(originalText)) {
                                rightMarkupModel.addRangeHighlighter(
                                        marker.getStartOffset(), marker.getEndOffset(),
                                        HighlighterLayer.SELECTION - 1, diffAttr, HighlighterTargetArea.EXACT_RANGE
                                );
                            }
                        }
                    }
                });
            }
        }, docListenerDisposable);


        leftEditor.getScrollingModel().addVisibleAreaListener(e -> {
            int targetY = e.getNewRectangle().y;
            if (rightEditor.getScrollingModel().getVerticalScrollOffset() != targetY) {
                rightEditor.getScrollingModel().scrollVertically(targetY);
            }
        });
        rightEditor.getScrollingModel().addVisibleAreaListener(e -> {
            int targetY = e.getNewRectangle().y;
            if (leftEditor.getScrollingModel().getVerticalScrollOffset() != targetY) {
                leftEditor.getScrollingModel().scrollVertically(targetY);
            }
        });

        rightEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(@NotNull EditorMouseEvent event) {
                MouseEvent e = event.getMouseEvent();
                if (e.isControlDown() || e.isMetaDown()) {
                    VisualPosition visualPos = rightEditor.xyToVisualPosition(e.getPoint());
                    int offset = rightEditor.logicalPositionToOffset(rightEditor.visualToLogicalPosition(visualPos));

                    boolean isInsideEditable = false;
                    for (RangeMarker m : valueMarkers) {
                        if (offset >= m.getStartOffset() && offset <= m.getEndOffset()) {
                            isInsideEditable = true;
                            break;
                        }
                    }
                    if (!isInsideEditable) {
                        offset = getNearestValidOffset(offset, valueMarkers);
                        LogicalPosition logPos = rightEditor.offsetToLogicalPosition(offset);
                        visualPos = rightEditor.logicalToVisualPosition(logPos);
                    }

                    CaretModel caretModel = rightEditor.getCaretModel();
                    Caret existingCaret = caretModel.getCaretAt(visualPos);
                    if (existingCaret != null) {
                        if (caretModel.getCaretCount() > 1) caretModel.removeCaret(existingCaret);
                    } else {
                        caretModel.addCaret(visualPos, true);
                    }
                    event.consume();
                }
            }
        });

        JBSplitter splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(leftEditor.getComponent());
        splitter.setSecondComponent(rightEditor.getComponent());

        JBPanel<?> statusBar = new JBPanel<>(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        JBLabel shortcutLabel = new JBLabel("💡 Shortcuts:  [Enter] Save   |   [Tab]/[↓] Next   |   [Ctrl+Click] Multi-Caret   |   [Ctrl+Shift+A] All Carets");
        shortcutLabel.setForeground(JBColor.GRAY);
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        statusBar.add(shortcutLabel, BorderLayout.WEST);

        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        DialogStyle.styleContent(panel);
        panel.add(splitter, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(JBUI.scale(1000), JBUI.scale(450)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, rightEditor.getContentComponent())
                .setTitle(getPopupTitle())
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setCancelOnWindowDeactivation(false)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        Runnable saveLogic = () -> {
            List<String> newValues = new ArrayList<>();
            for (int i = 0; i < selectedItems.size(); i++) {
                RangeMarker marker = valueMarkers.get(i);
                if (marker.isValid()) {
                    String newText = rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
                    // The editor shows newlines flattened to spaces (escapeJson); writing an
                    // untouched row back would permanently flatten the stored multi-line value.
                    // null = "unchanged, skip".
                    String originalEscaped = escapeJson(getOriginalValue(selectedItems.get(i)));
                    newValues.add(newText.equals(originalEscaped) ? null : unescapeJson(newText).trim());
                } else {
                    newValues.add(null);
                }
            }

            applyValues(selectedItems, newValues);
            if (updatedItems != null)
                // todo, apply update automation edit bulk test cases. set to null for now
                updatedItems.accept(selectedItems);
            popup.closeOk(null);
        };

        final JComponent keymapTarget = rightEditor.getContentComponent();
        KeyAction.register(saveLogic, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), keymapTarget);
        KeyAction.register(saveLogic, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), keymapTarget);
        KeyAction.register(() -> navigate(1, true, rightEditor, valueMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), keymapTarget);
        KeyAction.register(() -> navigate(-1, true, rightEditor, valueMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), keymapTarget);
        KeyAction.register(() -> navigate(1, false, rightEditor, valueMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), keymapTarget);
        KeyAction.register(() -> navigate(-1, false, rightEditor, valueMarkers), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), keymapTarget);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!rightEditor.isDisposed() && !leftEditor.isDisposed()) {
                if (!valueMarkers.isEmpty()) {
                    rightEditor.getCaretModel().moveToOffset(valueMarkers.getFirst().getEndOffset());
                }
                updateRowHighlights.run();
            }
        });

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                Disposer.dispose(docListenerDisposable);
                if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
                if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
            }
        });

        KeyAction.register(() -> {
            placeCaretOnAllValues(rightEditor, valueMarkers);
            updateRowHighlights.run();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | KeyEvent.SHIFT_DOWN_MASK), keymapTarget);

        popup.showCenteredInCurrentWindow(p);
    }

    /**
     * Moves the caret to the neighbouring editable value. Uses the live marker
     * offsets, so navigation stays correct while the user is typing.
     */
    private void navigate(final int direction, final boolean wrap, final Editor editor, final List<RangeMarker> markers) {
        if (markers.isEmpty()) return;

        editor.getCaretModel().removeSecondaryCarets();
        final int offset = editor.getCaretModel().getOffset();
        int currentIndex = 0;
        for (int i = 0; i < markers.size(); i++) {
            if (offset >= markers.get(i).getStartOffset() && offset <= markers.get(i).getEndOffset()) {
                currentIndex = i;
                break;
            }
        }

        final int target = wrap
                ? (currentIndex + direction + markers.size()) % markers.size()
                : currentIndex + direction;
        if (target < 0 || target >= markers.size()) return;

        editor.getCaretModel().moveToOffset(markers.get(target).getEndOffset());
    }

    /**
     * Ctrl+Shift+A: one caret at the end of every editable value.
     */
    private void placeCaretOnAllValues(final Editor editor, final List<RangeMarker> markers) {
        final CaretModel caretModel = editor.getCaretModel();
        caretModel.removeSecondaryCarets();

        boolean isFirst = true;
        for (final RangeMarker marker : markers) {
            if (!marker.isValid()) continue;

            final LogicalPosition logPos = editor.offsetToLogicalPosition(marker.getEndOffset());
            final VisualPosition visPos = editor.logicalToVisualPosition(logPos);

            if (isFirst) {
                caretModel.moveToVisualPosition(visPos);
                isFirst = false;
            } else {
                caretModel.addCaret(visPos, true);
            }
        }
    }

    private int getNearestValidOffset(final int offset, final List<RangeMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (RangeMarker m : markers) {
            if (Math.abs(offset - m.getStartOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getStartOffset());
                nearestOffset = m.getStartOffset();
            }
            if (Math.abs(offset - m.getEndOffset()) < minDistance) {
                minDistance = Math.abs(offset - m.getEndOffset());
                nearestOffset = m.getEndOffset();
            }
        }
        return nearestOffset;
    }

    private void setupEditorAppearance(final Editor editor, final @NotNull Project p) {
        FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(p, new com.intellij.testFramework.LightVirtualFile("dummy.json", jsonFileType, ""));

        if (editor instanceof EditorEx) {
            ((EditorEx) editor).setHighlighter(highlighter);
        }

        EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15f);
        scheme.setLineSpacing(1.4f);

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(true);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(1);
    }

    protected String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    protected String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
