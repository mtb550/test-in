package testGit.ui.bulk;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.repository.PersistenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

public class StepsBulkEditor {

    public static void show(List<TestCaseDto> selectedItems, Runnable onUpdate) {
        Project project = Config.getProject();
        if (project == null) return;

        // يمكنك هنا جلب خطوات أول عنصر كقالب (Template) إذا أردت:
        // String initialText = selectedItems.get(0).getSteps();
        String initialText = "";

        Document document = EditorFactory.getInstance().createDocument(initialText);
        Editor editor = EditorFactory.getInstance().createEditor(document, project);

        // 🌟 إعدادات المحرر لتتناسب مع كتابة الخطوات (خط كبير، التفاف النص، أرقام الأسطر)
        EditorColorsScheme scheme = editor.getColorsScheme();
        scheme.setEditorFontSize(18);
        scheme.setLineSpacing(1.6f);

        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true); // مهم جداً لمعرفة رقم الخطوة
        settings.setLineMarkerAreaShown(false);
        settings.setFoldingOutlineShown(false);
        settings.setVirtualSpace(false);
        settings.setUseSoftWraps(true); // تفعيل التفاف النص لأن الخطوات قد تكون طويلة
        settings.setAdditionalLinesCount(1); // مساحة إضافية في الأسفل لراحة العين

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(editor.getComponent(), BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(JBUI.scale(800), JBUI.scale(350)));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, editor.getContentComponent())
                .setTitle("Bulk Edit Steps (Press Ctrl+Enter to Save) - Overwrites " + selectedItems.size() + " items")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // 🌟 دالة الحفظ
        Runnable saveLogic = () -> {
            // نقوم بأخذ كل الأسطر كـ List (كل سطر يعتبر خطوة)
            List<String> newSteps = Arrays.asList(document.getText().split("\n"));

            // إرسال البيانات للحفظ
            PersistenceManager.updateSteps(selectedItems, newSteps, onUpdate);
            popup.closeOk(null);
        };

        // 🌟 نستخدم Ctrl+Enter للحفظ، لأن Enter العادي مطلوب للنزول لخطوة جديدة!
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                saveLogic.run();
            }
        }.registerCustomShortcutSet(new com.intellij.openapi.actionSystem.CustomShortcutSet(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK)), editor.getContentComponent());

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