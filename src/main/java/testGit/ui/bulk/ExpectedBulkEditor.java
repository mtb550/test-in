package testGit.ui.bulk;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

public class ExpectedBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        Project project = Config.getProject();
        if (project == null) return;

        int maxLen = 0;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < selectedItems.size(); i++) {
            // 🌟 تجهيز النص القابل للتعديل (Expected Results)
            String expected = selectedItems.get(i).getExpected();
            sb.append(expected != null ? expected : ""); // تجنب طباعة كلمة null

            if (i < selectedItems.size() - 1) sb.append("\n");

            // حساب عرض العمود الأيسر بناءً على أطوال العناوين (بحد أقصى 40 حرف)
            String title = selectedItems.get(i).getTitle();
            if (title != null) {
                maxLen = Math.max(maxLen, Math.min(title.length(), 40));
            }
        }

        final int gutterWidth = Math.max(maxLen, 30); // عرض مريح افتراضياً

        Document document = EditorFactory.getInstance().createDocument(sb.toString());

        // 🌟 منع إضافة أو إزالة أسطر بشكل قاطع للحفاظ على التزامن مع الـ List
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void beforeDocumentChange(@NotNull DocumentEvent event) {
                long newLinesAdded = event.getNewFragment().toString().chars().filter(ch -> ch == '\n').count();
                long newLinesRemoved = event.getOldFragment().toString().chars().filter(ch -> ch == '\n').count();

                if (newLinesAdded != newLinesRemoved) {
                    throw new ReadOnlyModificationException(document, "Cannot add or remove lines in bulk edit mode.");
                }
            }
        });

        Editor editor = EditorFactory.getInstance().createEditor(document, project);

        EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(20);
        scheme.setLineSpacing(1.8f);

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(false);
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalColumnsCount(0);

        // تعطيل زر Enter العادي
        editor.getContentComponent().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");

        // 🌟 دعم Multi-Caret عن طريق الـ Ctrl+Click
        editor.addEditorMouseListener(new EditorMouseListener() {
            @Override
            public void mousePressed(@NotNull EditorMouseEvent event) {
                MouseEvent e = event.getMouseEvent();
                if (e.isControlDown() || e.isMetaDown()) {
                    CaretModel caretModel = editor.getCaretModel();
                    VisualPosition visualPos = editor.xyToVisualPosition(e.getPoint());
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

        if (editor instanceof EditorEx) {
            ((EditorEx) editor).getGutterComponentEx().registerTextAnnotation(new TextAnnotationGutterProvider() {
                @Nullable
                @Override
                public String getLineText(int line, Editor editor) {
                    if (line >= 0 && line < selectedItems.size()) {
                        String title = selectedItems.get(line).getTitle();
                        if (title == null) title = "";

                        // 🌟 قص العنوان إذا كان طويلاً جداً لكي لا يطمس مساحة الكتابة
                        if (title.length() > 40) {
                            title = title.substring(0, 37) + "...";
                        }
                        return String.format("%-" + gutterWidth + "s", title);
                    }
                    return null;
                }

                @Nullable
                @Override
                public String getToolTip(int line, Editor editor) {
                    return null;
                }

                @Override
                public EditorFontType getStyle(int line, Editor editor) {
                    return EditorFontType.PLAIN;
                }

                @Nullable
                @Override
                public ColorKey getColor(int line, Editor editor) {
                    return EditorColors.ANNOTATIONS_COLOR;
                }

                @Nullable
                @Override
                public Color getBgColor(int line, Editor editor) {
                    return null;
                }

                @Override
                public List<AnAction> getPopupActions(int line, Editor editor) {
                    return null;
                }

                @Override
                public void gutterClosed() {
                }
            });
            ((EditorEx) editor).getGutterComponentEx().revalidateMarkup();
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(editor.getComponent(), BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(JBUI.scale(900), JBUI.scale(350)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, editor.getContentComponent())
                .setTitle("Bulk Edit Expected Results (Press Enter to Save | Ctrl+Click for Multi-Caret)")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // 🌟 دالة الحفظ
        Runnable saveLogic = () -> {
            String[] newExpected = document.getText().split("\n");
            PersistenceManager.updateExpected(selectedItems, newExpected, onUpdate);
            popup.closeOk(null);
        };

        // تفعيل الـ Enter للحفظ المباشر
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), editor.getContentComponent());

        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                if (!editor.isDisposed()) {
                    EditorFactory.getInstance().releaseEditor(editor);
                }
            }
        });

        popup.showCenteredInCurrentWindow(project);
    }
}