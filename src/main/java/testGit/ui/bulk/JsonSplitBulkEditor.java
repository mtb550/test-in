package testGit.ui.bulk;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class JsonSplitBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate, JsonFieldConfig config) {
        Project project = Config.getProject();
        if (project == null) return;

        StringBuilder leftSb = new StringBuilder();
        StringBuilder rightSb = new StringBuilder();
        List<int[]> rightEditableRanges = new ArrayList<>();

        leftSb.append("[\n");
        rightSb.append("[\n");

        for (int i = 0; i < selectedItems.size(); i++) {
            boolean isLast = (i == selectedItems.size() - 1);
            // 🌟 بناء النص لكل شاشة بناءً على إعداداتها (Title أو Expected)
            config.appendJsonItem(selectedItems.get(i), i, isLast, leftSb, rightSb, rightEditableRanges);
        }

        leftSb.append("]");
        rightSb.append("]");

        Document leftDoc = EditorFactory.getInstance().createDocument(leftSb.toString());
        leftDoc.setReadOnly(true);
        Editor leftEditor = EditorFactory.getInstance().createViewer(leftDoc, project);
        setupEditorAppearance(leftEditor, project);

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

        Editor rightEditor = EditorFactory.getInstance().createEditor(rightDoc, project);
        setupEditorAppearance(rightEditor, project);

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
                if (caret == null) return;

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

        MarkupModel rightMarkupModel = rightEditor.getMarkupModel();
        rightDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                SwingUtilities.invokeLater(() -> {
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
                            String originalText = escapeJson(config.getOriginalValue(selectedItems.get(i)));

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
        });

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

        // ==========================================================
        // 🌟 شريط الحالة (Status Bar)
        // ==========================================================
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        JLabel shortcutLabel = new JLabel("💡 Shortcuts:  [Enter] Save   |   [Tab] / [↓] Next   |   [Shift+Tab] / [↑] Prev   |   [Ctrl+Click] Multi-Caret");
        shortcutLabel.setForeground(JBColor.GRAY);
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        statusBar.add(shortcutLabel, BorderLayout.WEST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(splitter, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(JBUI.scale(1000), JBUI.scale(450)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, rightEditor.getContentComponent())
                .setTitle(config.getPopupTitle())
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        Runnable saveLogic = () -> {
            List<String> newValues = new ArrayList<>();
            for (int i = 0; i < selectedItems.size(); i++) {
                RangeMarker marker = valueMarkers.get(i);
                if (marker.isValid()) {
                    String newText = rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
                    newValues.add(unescapeJson(newText).trim());
                } else {
                    newValues.add("");
                }
            }
            config.saveValues(selectedItems, newValues, onUpdate);
            popup.closeOk(null);
        };

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        ), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK)
        ), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                rightEditor.getCaretModel().removeSecondaryCarets();
                int offset = rightEditor.getCaretModel().getOffset();
                int currentIndex = 0;
                for (int i = 0; i < valueMarkers.size(); i++) {
                    if (offset >= valueMarkers.get(i).getStartOffset() && offset <= valueMarkers.get(i).getEndOffset()) {
                        currentIndex = i;
                        break;
                    }
                }
                int nextIndex = (currentIndex + 1) % valueMarkers.size();
                rightEditor.getCaretModel().moveToOffset(valueMarkers.get(nextIndex).getEndOffset());
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                rightEditor.getCaretModel().removeSecondaryCarets();
                int offset = rightEditor.getCaretModel().getOffset();
                int currentIndex = 0;
                for (int i = 0; i < valueMarkers.size(); i++) {
                    if (offset >= valueMarkers.get(i).getStartOffset() && offset <= valueMarkers.get(i).getEndOffset()) {
                        currentIndex = i;
                        break;
                    }
                }
                int prevIndex = (currentIndex - 1 + valueMarkers.size()) % valueMarkers.size();
                rightEditor.getCaretModel().moveToOffset(valueMarkers.get(prevIndex).getEndOffset());
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                rightEditor.getCaretModel().removeSecondaryCarets();
                int offset = rightEditor.getCaretModel().getOffset();
                int currentIndex = 0;
                for (int i = 0; i < valueMarkers.size(); i++) {
                    if (offset >= valueMarkers.get(i).getStartOffset() && offset <= valueMarkers.get(i).getEndOffset()) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex < valueMarkers.size() - 1) {
                    rightEditor.getCaretModel().moveToOffset(valueMarkers.get(currentIndex + 1).getEndOffset());
                }
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                rightEditor.getCaretModel().removeSecondaryCarets();
                int offset = rightEditor.getCaretModel().getOffset();
                int currentIndex = 0;
                for (int i = 0; i < valueMarkers.size(); i++) {
                    if (offset >= valueMarkers.get(i).getStartOffset() && offset <= valueMarkers.get(i).getEndOffset()) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex > 0) {
                    rightEditor.getCaretModel().moveToOffset(valueMarkers.get(currentIndex - 1).getEndOffset());
                }
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)), rightEditor.getContentComponent());

        SwingUtilities.invokeLater(() -> {
            if (!rightEditor.isDisposed() && !leftEditor.isDisposed()) {
                if (!valueMarkers.isEmpty()) {
                    rightEditor.getCaretModel().moveToOffset(valueMarkers.get(0).getEndOffset());
                }
                updateRowHighlights.run();
            }
        });

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
                if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
            }
        });

        popup.showCenteredInCurrentWindow(project);
    }

    private static int getNearestValidOffset(int offset, List<RangeMarker> markers) {
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

    private static void setupEditorAppearance(Editor editor, Project project) {
        FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        com.intellij.openapi.editor.highlighter.EditorHighlighter highlighter =
                com.intellij.openapi.editor.highlighter.EditorHighlighterFactory.getInstance().createEditorHighlighter(project, jsonFileType);
        if (editor instanceof EditorEx) {
            ((EditorEx) editor).setHighlighter(highlighter);
        }

        EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(15);
        scheme.setLineSpacing(1.4f);

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(true);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(1);
    }

    public static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    public static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // 🌟 واجهة الإعدادات (Config) لتخصيص محتوى الـ JSON وطريقة الحفظ لكل نوع
    public interface JsonFieldConfig {
        String getPopupTitle();

        String getOriginalValue(TestCaseDto tc);

        void appendJsonItem(TestCaseDto tc, int index, boolean isLast, StringBuilder leftSb, StringBuilder rightSb, List<int[]> rightEditableRanges);

        void saveValues(List<TestCaseDto> items, List<String> newValues, Runnable onUpdate);
    }
}