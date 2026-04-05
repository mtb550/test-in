package testGit.ui.single.nnew;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithAutoCompletion;
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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CreateTestCaseUI extends BaseCreateTestCase {

    public void show(final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        TestCaseDto dto = new TestCaseDto();
        final JBPopup[] popupWrapper = new JBPopup[1];

        UIAction repackPopup = () -> {
            if (popupWrapper[0] != null) popupWrapper[0].pack(true, true);
        };

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                pref.width = Math.max(pref.width, screenWidth / 2);
                return pref;
            }
        };
        mainPanel.setBorder(JBUI.Borders.empty());
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        // 1. Title
        titleSection.showSection(contentPanel); // show first
        ExtendableTextField titleField = titleSection.getField();
        JPanel titleWrapper = titleSection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseTitle.getShortcut(), () -> {
            titleSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 2. Expected
        ExtendableTextField expectedField = expectedSection.getField();
        JPanel expectedWrapper = expectedSection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getShortcut(), () -> {
            expectedSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 3. Priority
        ComboBox<Priority> priorityCombo = prioritySection.getCombo();
        JPanel priorityWrapper = prioritySection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCasePriority.getShortcut(), () -> {
            prioritySection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 4. Groups
        JPanel groupsPanel = groupsSection.getInnerPanel();
        JPanel groupsWrapper = groupsSection.getWrapper();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseGroups.getShortcut(), () -> {
            groupsSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 5. Steps
        JPanel stepsWrapper = stepsSection.getWrapper();
        List<TextFieldWithAutoCompletion<String>> stepFields = stepsSection.getStepFields();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseStep.getShortcut(), () ->
                stepsSection.showSection(contentPanel, repackPopup, uniqueStepsCache));

        // status bar
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(statusBarSection.getPanel(), BorderLayout.SOUTH);

        // بناء النافذة المنبثقة (Popup)
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, titleField)
                .setTitle("Create Test Case")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // تفويض منطق الحفظ ruunuable to be removed
        Runnable saveAction = SingleEditorSaveManager.createSaveAction(
                dto,
                titleWrapper,
                titleField,
                expectedWrapper,
                expectedField,
                priorityWrapper,
                priorityCombo,
                groupsWrapper,
                groupsPanel,
                stepsWrapper,
                stepFields,
                onSave,
                popupWrapper
        );

        // General Shortcuts (Tab Navigation & Save)
        registerShortcut(mainPanel, KeyboardSet.TabNext.getShortcut(), () ->
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());

        registerShortcut(mainPanel, KeyboardSet.TabPrevious.getShortcut(), () ->
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());

        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction::run);

        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
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

}