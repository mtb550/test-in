package testGit.ui.bulk;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
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
import com.intellij.openapi.project.DumbAwareAction;
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

public class StepsBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        Project project = Config.getProject();
        if (project == null) return;

        List<List<String>> originalSteps = new ArrayList<>();
        List<List<String>> activeSteps = new ArrayList<>();

        for (TestCaseDto tc : selectedItems) {
            List<String> current = tc.getSteps() != null ? new ArrayList<>(tc.getSteps()) : new ArrayList<>();
            if (current.isEmpty()) current.add("");
            originalSteps.add(new ArrayList<>(current));
            activeSteps.add(new ArrayList<>(current));
        }

        Document leftDoc = EditorFactory.getInstance().createDocument("");
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

        Document rightDoc = EditorFactory.getInstance().createDocument("");
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(rightDoc, e -> {
        });
        Editor rightEditor = EditorFactory.getInstance().createEditor(rightDoc, project);
        setupEditorAppearance(rightEditor, project);

        List<RangeHighlighter> leftLineHighlighters = new ArrayList<>();
        List<StepMarker> stepMarkers = new ArrayList<>();
        List<RangeMarker> guardBlocks = new ArrayList<>();

        Color themeCaretRowColor = rightEditor.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR);
        if (themeCaretRowColor == null)
            themeCaretRowColor = new JBColor(Gray._245, Gray._50);
        TextAttributes leftLineAttr = new TextAttributes();
        leftLineAttr.setBackgroundColor(themeCaretRowColor);

        Runnable syncStateFromEditor = () -> {
            for (StepMarker sm : stepMarkers) {
                if (sm.marker != null && sm.marker.isValid()) {
                    String text = rightDoc.getText(new TextRange(sm.marker.getStartOffset(), sm.marker.getEndOffset()));

                    activeSteps.get(sm.tcIdx).set(sm.stepIdx, unescapeJson(text));
                }
            }
        };

        BiConsumer<Integer, Integer> renderUI = (focusTc, focusStep) -> {
            StringBuilder leftSb = new StringBuilder("[\n");
            StringBuilder rightSb = new StringBuilder("[\n");
            List<StepMarker> tempMarkers = new ArrayList<>();

            for (int i = 0; i < selectedItems.size(); i++) {
                TestCaseDto tc = selectedItems.get(i);
                String id = "Item-" + (i + 1);
                String escapedTitle = escapeJson(tc.getTitle());

                String prefix = "  {\n    \"id\": \"" + id + "\",\n    \"title\": \"" + escapedTitle + "\",\n    \"steps\": [\n";
                leftSb.append(prefix);
                rightSb.append(prefix);

                List<String> origSteps = originalSteps.get(i);
                for (int j = 0; j < origSteps.size(); j++) {
                    String stepPrefix = "      \"";
                    String stepSuffix = "\"" + (j < origSteps.size() - 1 ? "," : "") + "\n";
                    leftSb.append(stepPrefix).append(escapeJson(origSteps.get(j))).append(stepSuffix);
                }

                List<String> currSteps = activeSteps.get(i);
                for (int j = 0; j < currSteps.size(); j++) {
                    String stepPrefix = "      \"";
                    String stepSuffix = "\"" + (j < currSteps.size() - 1 ? "," : "") + "\n";

                    rightSb.append(stepPrefix);

                    StepMarker sm = new StepMarker();
                    sm.tcIdx = i;
                    sm.stepIdx = j;
                    sm.startOffset = rightSb.length();
                    rightSb.append(escapeJson(currSteps.get(j)));
                    sm.endOffset = rightSb.length();
                    tempMarkers.add(sm);

                    rightSb.append(stepSuffix);
                }

                String suffix = "    ]\n  }";
                String comma = (i < selectedItems.size() - 1) ? ",\n" : "\n";
                leftSb.append(suffix).append(comma);
                rightSb.append(suffix).append(comma);
            }
            leftSb.append("]");
            rightSb.append("]");

            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (RangeMarker g : guardBlocks) rightDoc.removeGuardedBlock(g);
                guardBlocks.clear();
                stepMarkers.clear();

                leftDoc.setReadOnly(false);
                leftDoc.setText(leftSb.toString());
                leftDoc.setReadOnly(true);

                rightDoc.setText(rightSb.toString());

                int currentGuardStart = 0;
                for (StepMarker sm : tempMarkers) {
                    RangeMarker rm = rightDoc.createRangeMarker(sm.startOffset, sm.endOffset);
                    rm.setGreedyToLeft(true);
                    rm.setGreedyToRight(true);
                    sm.marker = rm;
                    stepMarkers.add(sm);

                    if (currentGuardStart < sm.startOffset) {
                        guardBlocks.add(rightDoc.createGuardedBlock(currentGuardStart, sm.startOffset));
                    }
                    currentGuardStart = sm.endOffset;
                }
                if (currentGuardStart < rightDoc.getTextLength()) {
                    guardBlocks.add(rightDoc.createGuardedBlock(currentGuardStart, rightDoc.getTextLength()));
                }
            });

            if (focusTc != null && focusStep != null) {
                for (StepMarker sm : stepMarkers) {
                    if (sm.tcIdx == focusTc && sm.stepIdx == focusStep) {
                        rightEditor.getCaretModel().moveToOffset(sm.marker.getEndOffset());
                        break;
                    }
                }
            }
        };

        Runnable updateRowHighlights = () -> {
            if (leftEditor.isDisposed() || rightEditor.isDisposed()) return;
            MarkupModel leftMarkup = leftEditor.getMarkupModel();
            for (RangeHighlighter h : leftLineHighlighters) leftMarkup.removeHighlighter(h);
            leftLineHighlighters.clear();

            for (Caret caret : rightEditor.getCaretModel().getAllCarets()) {
                int line = rightDoc.getLineNumber(caret.getOffset());
                if (line < leftDoc.getLineCount()) {
                    leftLineHighlighters.add(leftMarkup.addLineHighlighter(line, HighlighterLayer.CARET_ROW, leftLineAttr));
                }
            }
        };

        rightEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                Caret caret = event.getCaret();
                int offset = caret.getOffset();
                boolean isInsideEditable = stepMarkers.stream().anyMatch(m ->
                        m.marker != null && m.marker.isValid() && offset >= m.marker.getStartOffset() && offset <= m.marker.getEndOffset());
                if (!isInsideEditable) caret.moveToOffset(getNearestValidOffset(offset, stepMarkers));
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
        rightDoc.addDocumentListener(new DocumentListener() {
                                         @Override
                                         public void documentChanged(@NotNull DocumentEvent event) {
                                             SwingUtilities.invokeLater(() -> {
                                                 if (rightEditor.isDisposed()) return;
                                                 MarkupModel markupModel = rightEditor.getMarkupModel();
                                                 for (RangeHighlighter h : markupModel.getAllHighlighters()) {
                                                     if (h.getLayer() == HighlighterLayer.SELECTION - 1) markupModel.removeHighlighter(h);
                                                 }
                                                 TextAttributes diffAttr = new TextAttributes();
                                                 diffAttr.setBackgroundColor(new JBColor(new Color(228, 250, 228), new Color(43, 61, 44)));

                                                 for (StepMarker sm : stepMarkers) {
                                                     if (sm.marker != null && sm.marker.isValid()) {
                                                         String currentText = rightDoc.getText(new TextRange(sm.marker.getStartOffset(), sm.marker.getEndOffset()));
                                                         List<String> origList = originalSteps.get(sm.tcIdx);
                                                         String originalText = (sm.stepIdx < origList.size()) ? escapeJson(origList.get(sm.stepIdx)) : "";

                                                         if (!currentText.equals(originalText)) {
                                                             markupModel.addRangeHighlighter(sm.marker.getStartOffset(), sm.marker.getEndOffset(),
                                                                     HighlighterLayer.SELECTION - 1, diffAttr, HighlighterTargetArea.EXACT_RANGE);
                                                         }
                                                     }
                                                 }
                                             });
                                         }
                                     }, docListenerDisposable
        );

        rightEditor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(@NotNull EditorMouseEvent event) {
                MouseEvent e = event.getMouseEvent();
                if (e.isControlDown() || e.isMetaDown()) {
                    VisualPosition visualPos = rightEditor.xyToVisualPosition(e.getPoint());
                    int offset = rightEditor.logicalPositionToOffset(rightEditor.visualToLogicalPosition(visualPos));
                    int finalOffset = offset;
                    boolean isInsideEditable = stepMarkers.stream().anyMatch(m ->
                            m.marker != null && m.marker.isValid() && finalOffset >= m.marker.getStartOffset() && finalOffset <= m.marker.getEndOffset());

                    if (!isInsideEditable) {
                        offset = getNearestValidOffset(offset, stepMarkers);
                        visualPos = rightEditor.logicalToVisualPosition(rightEditor.offsetToLogicalPosition(offset));
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

        leftEditor.getScrollingModel().addVisibleAreaListener(e -> rightEditor.getScrollingModel().scrollVertically(e.getNewRectangle().y));
        rightEditor.getScrollingModel().addVisibleAreaListener(e -> leftEditor.getScrollingModel().scrollVertically(e.getNewRectangle().y));

        JBSplitter splitter = new JBSplitter(false, 0.5f);
        splitter.setFirstComponent(leftEditor.getComponent());
        splitter.setSecondComponent(rightEditor.getComponent());

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        JLabel shortcutLabel = new JLabel("💡 Shortcuts:  [Enter] Save   |   [Ctrl+Enter] Add Step   |   [Shift+Delete] Remove Step   |   [Tab] / [↓] Next   |   [Ctrl+Click] Multi-Caret");
        shortcutLabel.setForeground(JBColor.GRAY);
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        statusBar.add(shortcutLabel, BorderLayout.WEST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(splitter, BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(JBUI.scale(1100), JBUI.scale(550)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, rightEditor.getContentComponent())
                .setTitle("Bulk Edit Steps")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        Runnable saveLogic = () -> {
            syncStateFromEditor.run();
            PersistenceManager.updateSteps(selectedItems, activeSteps, onUpdate);
            popup.closeOk(null);
        };

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                syncStateFromEditor.run();
                List<Caret> carets = rightEditor.getCaretModel().getAllCarets();
                List<int[]> targets = new ArrayList<>();

                for (Caret caret : carets) {
                    int offset = caret.getOffset();
                    StepMarker current = stepMarkers.stream().filter(m -> m.marker != null && offset >= m.marker.getStartOffset() && offset <= m.marker.getEndOffset()).findFirst().orElse(null);
                    if (current != null) {
                        if (targets.stream().noneMatch(arr -> arr[0] == current.tcIdx && arr[1] == current.stepIdx)) {
                            targets.add(new int[]{current.tcIdx, current.stepIdx});
                        }
                    }
                }
                if (targets.isEmpty()) return;

                targets.sort((a, b) -> {
                    if (a[0] != b[0]) return Integer.compare(b[0], a[0]);
                    return Integer.compare(b[1], a[1]);
                });

                for (int[] target : targets) {
                    activeSteps.get(target[0]).add(target[1] + 1, "");
                }

                int[] firstAdded = targets.getLast();
                renderUI.accept(firstAdded[0], firstAdded[1] + 1);
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                syncStateFromEditor.run();
                List<Caret> carets = rightEditor.getCaretModel().getAllCarets();
                List<int[]> targets = new ArrayList<>();

                for (Caret caret : carets) {
                    int offset = caret.getOffset();
                    StepMarker current = stepMarkers.stream().filter(m -> m.marker != null && offset >= m.marker.getStartOffset() && offset <= m.marker.getEndOffset()).findFirst().orElse(null);
                    if (current != null) {
                        if (targets.stream().noneMatch(arr -> arr[0] == current.tcIdx && arr[1] == current.stepIdx)) {
                            targets.add(new int[]{current.tcIdx, current.stepIdx});
                        }
                    }
                }
                if (targets.isEmpty()) return;

                targets.sort((a, b) -> {
                    if (a[0] != b[0]) return Integer.compare(b[0], a[0]);
                    return Integer.compare(b[1], a[1]);
                });

                int focusTc = targets.getLast()[0];
                int focusStep = 0;

                for (int[] target : targets) {
                    List<String> steps = activeSteps.get(target[0]);
                    if (steps.size() > 1) {
                        steps.remove(target[1]);
                        focusStep = Math.min(target[1], steps.size() - 1);
                    } else {
                        steps.set(0, "");
                        focusStep = 0;
                    }
                }
                renderUI.accept(focusTc, focusStep);
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_DOWN_MASK)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                navigate(1, rightEditor, stepMarkers);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                navigate(1, rightEditor, stepMarkers);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                navigate(-1, rightEditor, stepMarkers);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)), rightEditor.getContentComponent());

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                navigate(-1, rightEditor, stepMarkers);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)), rightEditor.getContentComponent());

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (!leftEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(leftEditor);
                if (!rightEditor.isDisposed()) EditorFactory.getInstance().releaseEditor(rightEditor);
            }
        });

        renderUI.accept(0, 0);
        popup.showCenteredInCurrentWindow(project);
    }

    private static void navigate(int direction, Editor editor, List<StepMarker> markers) {
        editor.getCaretModel().removeSecondaryCarets();
        int offset = editor.getCaretModel().getOffset();
        int currentIndex = 0;
        for (int i = 0; i < markers.size(); i++) {
            if (offset >= markers.get(i).startOffset && offset <= markers.get(i).endOffset) {
                currentIndex = i;
                break;
            }
        }
        int targetIndex = (currentIndex + direction + markers.size()) % markers.size();
        editor.getCaretModel().moveToOffset(markers.get(targetIndex).endOffset);
    }

    private static int getNearestValidOffset(int offset, List<StepMarker> markers) {
        int minDistance = Integer.MAX_VALUE;
        int nearestOffset = offset;
        for (StepMarker m : markers) {
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

    private static void setupEditorAppearance(Editor editor, Project project) {
        FileType jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json");
        com.intellij.openapi.editor.highlighter.EditorHighlighter highlighter = com.intellij.openapi.editor.highlighter.EditorHighlighterFactory.getInstance().createEditorHighlighter(project, jsonFileType);
        if (editor instanceof EditorEx) ((EditorEx) editor).setHighlighter(highlighter);
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

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private interface BiConsumer<T, U> {
        void accept(T t, U u);
    }

    private static class StepMarker {
        RangeMarker marker;
        int tcIdx;
        int stepIdx;
        int startOffset;
        int endOffset;
    }
}