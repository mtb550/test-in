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
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TitleBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        Project project = Config.getProject();
        if (project == null) return;

        StringBuilder leftSb = new StringBuilder();
        StringBuilder rightSb = new StringBuilder();
        List<int[]> rightEditableRanges = new ArrayList<>();

        leftSb.append("[\n");
        rightSb.append("[\n");

        for (int i = 0; i < selectedItems.size(); i++) {
            TestCaseDto tc = selectedItems.get(i);
            String id = "Item-" + (i + 1);
            String escapedTitle = escapeJson(tc.getTitle());

            String itemPrefix = "  {\n    \"id\": \"" + id + "\",\n    \"title\": \"";
            String itemSuffix = "\"\n  }";
            String comma = (i < selectedItems.size() - 1) ? ",\n" : "\n";

            leftSb.append(itemPrefix).append(escapedTitle).append(itemSuffix).append(comma);

            rightSb.append(itemPrefix);
            int startOffset = rightSb.length();
            rightSb.append(escapedTitle);
            int endOffset = rightSb.length();
            rightEditableRanges.add(new int[]{startOffset, endOffset});
            rightSb.append(itemSuffix).append(comma);
        }

        leftSb.append("]");
        rightSb.append("]");

        // ==========================================================
        // 1. إعداد العمود الأيسر (للعرض فقط)
        // ==========================================================
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

        // ==========================================================
        // 2. إعداد العمود الأيمن وتطبيق الحماية (Guarded Blocks)
        // ==========================================================
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

        // ==========================================================
        // 🌟 مزامنة لون السطر الديناميكية (بدون تكرار)
        // ==========================================================
        List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();
        Color themeCaretRowColor = rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (themeCaretRowColor == null) {
            themeCaretRowColor = new JBColor(new Color(245, 245, 245), new Color(50, 50, 50));
        }
        TextAttributes leftLineAttr = new TextAttributes();
        leftLineAttr.setBackgroundColor(themeCaretRowColor);

        Runnable updateRowHighlights = () -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;
            MarkupModel leftMarkup = leftEditor.getMarkupModel();

            // مسح التظليلات القديمة لتجنب التكرار
            for (RangeHighlighter h : leftLineHighlighters) {
                leftMarkup.removeHighlighter(h);
            }
            leftLineHighlighters.clear();

            // إضافة التظليل للأسطر الحالية (يدعم Multi-Caret)
            for (Caret caret : rightEditor.getCaretModel().getAllCarets()) {
                int line = rightDoc.getLineNumber(caret.getOffset());
                if (line < leftDoc.getLineCount()) {
                    leftLineHighlighters.add(leftMarkup.addLineHighlighter(
                            line, HighlighterLayer.CARET_ROW, leftLineAttr
                    ));
                }
            }
        };

        // مغنطة المؤشر وتحديث الألوان
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

        // التظليل عند التعديل (Diff Highlight)
        MarkupModel rightMarkupModel = rightEditor.getMarkupModel();
        rightDoc.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                SwingUtilities.invokeLater(() -> {
                    if (rightEditor.isDisposed()) return;

                    for (RangeHighlighter h : rightMarkupModel.getAllHighlighters()) {
                        if (h.getLayer() == HighlighterLayer.SELECTION - 1) {
                            rightMarkupModel.removeHighlighter(h);
                        }
                    }

                    TextAttributes diffAttr = new TextAttributes();
                    diffAttr.setBackgroundColor(new JBColor(new Color(228, 250, 228), new Color(43, 61, 44)));

                    for (int i = 0; i < selectedItems.size(); i++) {
                        RangeMarker marker = valueMarkers.get(i);
                        if (marker.isValid()) {
                            String currentText = rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
                            String originalText = escapeJson(selectedItems.get(i).getTitle());

                            if (!currentText.equals(originalText)) {
                                rightMarkupModel.addRangeHighlighter(
                                        marker.getStartOffset(),
                                        marker.getEndOffset(),
                                        HighlighterLayer.SELECTION - 1,
                                        diffAttr,
                                        HighlighterTargetArea.EXACT_RANGE
                                );
                            }
                        }
                    }
                });
            }
        });

        // مزامنة التمرير
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

        // ==========================================================
        // 🌟 حماية الـ Multi-Caret وسحبه لأقرب حقل
        // ==========================================================
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

                    // إذا نقر بالخارج، اسحب المؤشر الإضافي لأقرب حقل
                    if (!isInsideEditable) {
                        offset = getNearestValidOffset(offset, valueMarkers);
                        LogicalPosition logPos = rightEditor.offsetToLogicalPosition(offset);
                        visualPos = rightEditor.logicalToVisualPosition(logPos);
                    }

                    CaretModel caretModel = rightEditor.getCaretModel();
                    Caret existingCaret = caretModel.getCaretAt(visualPos);
                    if (existingCaret != null) {
                        if (caretModel.getCaretCount() > 1) {
                            caretModel.removeCaret(existingCaret);
                        }
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

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(splitter, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(JBUI.scale(1000), JBUI.scale(450)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, rightEditor.getContentComponent())
                .setTitle("Bulk Edit Titles (Enter to Save | Tab/Arrows to Navigate)")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        Runnable saveLogic = () -> {
            String[] newTitles = new String[selectedItems.size()];
            for (int i = 0; i < selectedItems.size(); i++) {
                RangeMarker marker = valueMarkers.get(i);
                if (marker.isValid()) {
                    String newTitleText = rightDoc.getText(new TextRange(marker.getStartOffset(), marker.getEndOffset()));
                    newTitles[i] = unescapeJson(newTitleText).trim();
                } else {
                    newTitles[i] = "";
                }
            }
            PersistenceManager.updateTitles(selectedItems, newTitles, onUpdate);
            popup.closeOk(null);
        };

        // ==========================================================
        // اختصارات لوحة المفاتيح
        // ==========================================================
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


        // 🌟 تعيين المؤشر المبدئي وتلوين الخلفية الفوري
        SwingUtilities.invokeLater(() -> {
            if (!rightEditor.isDisposed() && !leftEditor.isDisposed()) {
                if (!valueMarkers.isEmpty()) {
                    rightEditor.getCaretModel().moveToOffset(valueMarkers.get(0).getEndOffset());
                }
                updateRowHighlights.run(); // استدعاء الدالة لتلوين السطر الأول من البداية
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

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}