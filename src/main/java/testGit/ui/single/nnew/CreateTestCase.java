package testGit.ui.single.nnew;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.bulk.UpdateField;
import testGit.ui.single.SingleEditorSaveManager;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CreateTestCase extends BaseCreateTestCase {

    public void show(final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        Project project = Config.getProject();
        TestCaseDto dto = new TestCaseDto();
        final JBPopup[] popupWrapper = new JBPopup[1];
        Runnable repackPopup = () -> {
            if (popupWrapper[0] != null) popupWrapper[0].pack(true, true);
        };

        JPanel mainPanel = createMainPanel();
        JPanel contentPanel = createContentPanel();

        // title
        titleSection.appendTo(contentPanel);
        ExtendableTextField titleField = titleSection.getField();

        // expected
        ExtendableTextField expectedField = expectedSection.getField();
        registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.EXPECTED.getShortcut(), InputEvent.CTRL_DOWN_MASK), expectedSection.getShowAction(contentPanel, repackPopup));
        JPanel expectedWrapper = expectedSection.getWrapper();

        // priority
        ComboBox<Priority> priorityCombo = prioritySection.getCombo();
        JPanel priorityWrapper = prioritySection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.PRIORITY.getShortcut(), InputEvent.CTRL_DOWN_MASK), prioritySection.getShowAction(contentPanel, repackPopup));

        // groups
        JPanel groupsPanel = groupsSection.getInnerPanel();
        JPanel groupsWrapper = groupsSection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.GROUPS.getShortcut(), InputEvent.CTRL_DOWN_MASK), groupsSection.getShowAction(contentPanel, repackPopup));

        // 5. Steps
        JPanel stepsWrapper = stepsSection.getWrapper();
        List<TextFieldWithAutoCompletion<String>> stepFields = stepsSection.getStepFields();
        registerShortcut(mainPanel, KeyboardSet.getShortcutFor(UpdateField.STEPS.getShortcut(), InputEvent.CTRL_DOWN_MASK), stepsSection.getShowAction(contentPanel, repackPopup, uniqueStepsCache));

        // status bar
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(createStatusBar(getCreateShortcutLabel()), BorderLayout.SOUTH);

        // بناء النافذة المنبثقة (Popup)
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, titleField)
                .setTitle("Create Test Case")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // تفويض منطق الحفظ
        Runnable saveAction = SingleEditorSaveManager.createSaveAction(
                dto, titleField, expectedWrapper, expectedField, priorityWrapper, priorityCombo,
                groupsWrapper, groupsPanel, stepsWrapper, stepFields, onSave, popupWrapper
        );

        // General Shortcuts (Tab Navigation & Save)
        registerShortcut(mainPanel, KeyboardSet.TabNext.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());
        registerShortcut(mainPanel, KeyboardSet.TabPrevious.getShortcut(), () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());
        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction);

        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                pref.width = Math.max(pref.width, screenWidth / 2); // العرض يكون دائماً نصف الشاشة في وضع الإنشاء
                return pref;
            }
        };
        mainPanel.setBorder(JBUI.Borders.empty());
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        return mainPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));
        return contentPanel;
    }

    private JPanel createStatusBar(JLabel shortcutLabel) {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        statusBar.setOpaque(true);
        statusBar.setBackground(UIUtil.getPanelBackground());
        statusBar.add(shortcutLabel, BorderLayout.WEST);
        return statusBar;
    }

    private @NotNull JLabel getCreateShortcutLabel() {
        String shortcutText = String.format("💡 [Enter] Save   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s",
                UpdateField.EXPECTED.getShortcut(), UpdateField.EXPECTED.getLabel(),
                UpdateField.STEPS.getShortcut(), UpdateField.STEPS.getLabel(),
                UpdateField.PRIORITY.getShortcut(), UpdateField.PRIORITY.getLabel(),
                UpdateField.GROUPS.getShortcut(), UpdateField.GROUPS.getLabel());
        JLabel label = new JLabel(shortcutText);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(JBColor.GRAY);
        return label;
    }

    // unused
    private void focusFirstCheckbox(final JPanel groupsWrapper) {
        for (Component child : groupsWrapper.getComponents()) {
            if (child instanceof JPanel groupsPanel) {
                for (Component c : groupsPanel.getComponents()) {
                    if (c instanceof JBCheckBox cb) {
                        SwingUtilities.invokeLater(cb::requestFocusInWindow);
                        return;
                    }
                }
            }
        }
    }
}