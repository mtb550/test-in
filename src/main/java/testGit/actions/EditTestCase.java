package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.GenericSelectionPopup;
import testGit.util.KeyboardSet;
import testGit.viewPanel.TestCaseDetailsPanel;
import testGit.viewPanel.ToolWindowFactoryImpl;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class EditTestCase extends DumbAwareAction {

    private final JBList<TestCaseDto> list;
    private final Path path;
    private final TestCaseDetailsPanel panelContext;

    public EditTestCase(final JBList<TestCaseDto> list, final Path path) {
        super("Edit Test Case");
        this.list = list;
        this.path = path;
        this.panelContext = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), list);

        /// to be implemented. to update the test case from UI without use view panel
        //this.registerCustomShortcutSet(KeyboardSet.UpdateTestCaseFase.getShortcut(), list);
    }

    public EditTestCase(final TestCaseDetailsPanel panelContext, final JComponent targetComponent) {
        super("Edit Test Case");
        this.panelContext = panelContext;
        this.list = null;
        this.path = null;
        this.registerCustomShortcutSet(KeyboardSet.UpdateTestCase.getShortcut(), targetComponent);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        // 1. التعديل من داخل نافذة التفاصيل
        if (panelContext != null) {
            if (!panelContext.isEditing()) {
                panelContext.toggleEditMode(true);
            }
            return;
        }

        // 2. التعديل من القائمة (JBList)
        if (list != null) {
            List<TestCaseDto> selectedItems = list.getSelectedValuesList();

            if (selectedItems.isEmpty()) return;

            // 🌟 مسار الـ Multi-Edit (تحديد أكثر من عنصر)
            if (selectedItems.size() > 1) {
                showMultiEditPopup(selectedItems);
            }
            // 🌟 مسار التعديل الفردي العادي
            else {
                TestCaseDto targetDto = selectedItems.get(0);
                ViewPanel.show(targetDto, path);

                SwingUtilities.invokeLater(() -> {
                    TestCaseDetailsPanel detailsPanel = ToolWindowFactoryImpl.getDetailsInstance();
                    if (detailsPanel != null && !detailsPanel.isEditing()) {
                        detailsPanel.toggleEditMode(true);
                    }
                });
            }
        }
    }

    // ==========================================================
    // منطق الـ Multi-Edit Popups
    // ==========================================================

    private void showMultiEditPopup(List<TestCaseDto> selectedItems) {
        GenericSelectionPopup.show(
                "Update " + selectedItems.size() + " Test Cases",
                UpdateField.values(),
                UpdateField::getLabel,
                UpdateField::getShortcut,
                selectedField -> {
                    if (selectedField == UpdateField.PRIORITY) {
                        showPrioritySelectionPopup(selectedItems);
                    } else {
                        // سنكمل الباقي لاحقاً كما طلبت
                        System.out.println("Selected: " + selectedField.getLabel() + " (To be implemented)");
                    }
                }
        );
    }

    private void showPrioritySelectionPopup(List<TestCaseDto> selectedItems) {
        GenericSelectionPopup.show(
                "Select Priority",
                Priority.values(),
                Priority::name, // أو Priority::name حسب ما تفضله لعرض الاسم
                p -> p.name().charAt(0),  // أخذ أول حرف كاختصار (مثلاً H لـ High، M لـ Medium)
                selectedPriority -> {
                    // تطبيق الأولوية الجديدة على جميع العناصر المحددة
                    for (TestCaseDto tc : selectedItems) {
                        tc.setPriority(selectedPriority);
                    }

                    // تحديث القائمة في واجهة المستخدم
                    list.repaint();

                    // TODO: يمكنك هنا إضافة الاستدعاء الخاص بحفظ التعديلات في قاعدة البيانات
                    System.out.println("Priority updated to " + selectedPriority + " for " + selectedItems.size() + " test cases.");
                }
        );
    }

    // ==========================================================
    // Enum لتحديد الحقول مع اختصاراتها الدقيقة
    // ==========================================================
    public enum UpdateField {
        TITLE("Title", 'T'),
        EXPECTED("Expected Results", 'E'),
        STEPS("Steps", 'S'),
        PRIORITY("Priority", 'P');
        //SEVERITY("Severity", 's'); // حرف صغير لتفريقه عن Steps /// not here. to be in test run editor

        private final String label;
        private final char shortcut;

        UpdateField(String label, char shortcut) {
            this.label = label;
            this.shortcut = shortcut;
        }

        public String getLabel() {
            return label;
        }

        public char getShortcut() {
            return shortcut;
        }
    }
}