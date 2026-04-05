package testGit.ui.single;

import com.intellij.openapi.project.Project;
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
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static testGit.ui.single.SingleEditorUIFactory.*;

public class SingleTestCaseEditor {

    public static void showForCreate(final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        buildAndShow(new TestCaseDto(), true, null, onSave, uniqueStepsCache);
    }

    public static void showForEdit(final TestCaseDto existingDto, final UpdateField targetField, final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        buildAndShow(existingDto, false, targetField, onSave, uniqueStepsCache);
    }

    private static void buildAndShow(final TestCaseDto dto, final boolean isExtendable, final UpdateField targetField, final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        Project project = Config.getProject();
        final JBPopup[] popupWrapper = new JBPopup[1];
        Runnable repackPopup = () -> {
            if (popupWrapper[0] != null) popupWrapper[0].pack(true, true);
        };

        // Panel Setup
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
                int targetWidth = (isExtendable || targetField == UpdateField.TITLE || targetField == UpdateField.EXPECTED)
                        ? (screenWidth / 2) : JBUI.scale(600);
                pref.width = Math.max(pref.width, targetWidth);
                return pref;
            }
        };
        mainPanel.setBorder(JBUI.Borders.empty());
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        // 1. Read-Only Context
        ExtendableTextField titleField = null;
        if (!isExtendable && targetField != null) {
            if (targetField != UpdateField.TITLE)
                contentPanel.add(createReadOnlyField(dto.getTitle(), UpdateField.TITLE.getIcon(), TITLE_FONT_SIZE));

            if (targetField == UpdateField.STEPS && dto.getExpected() != null && !dto.getExpected().isEmpty())
                contentPanel.add(createReadOnlyField("Expected: " + dto.getExpected(), UpdateField.EXPECTED.getIcon(), FIELD_FONT_SIZE));
        }

        // 2. Editable Fields
        JPanel titleWrapper = null;
        if (isExtendable || targetField == UpdateField.TITLE) {
            titleField = createTextField(UpdateField.TITLE.getLabel(), UpdateField.TITLE.getIcon(), TITLE_FONT_SIZE);
            titleWrapper = SingleEditorUIFactory.wrapComponent(titleField, null);

            if (dto.getTitle() != null) titleField.setText(dto.getTitle());
            contentPanel.add(titleWrapper);
        }

        ExtendableTextField expectedField = createTextField(UpdateField.EXPECTED.getLabel(), UpdateField.EXPECTED.getIcon(), FIELD_FONT_SIZE);
        JPanel expectedWrapper = wrapComponent(expectedField, null);

        ComboBox<Priority> priorityCombo = createPriorityCombo();
        priorityCombo.setSelectedItem(dto.getPriority() != null ? dto.getPriority() : Priority.LOW);
        JPanel priorityWrapper = wrapComponent(priorityCombo, UpdateField.PRIORITY);

        JPanel groupsPanel = createGroupsPanel();
        JPanel groupsWrapper = wrapComponent(groupsPanel, UpdateField.GROUPS);

        JPanel stepsContainer = new JPanel();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        stepsContainer.setOpaque(false);
        JPanel stepsWrapper = wrapComponent(stepsContainer, null);

        List<TextFieldWithAutoCompletion<String>> stepFields = new ArrayList<>();

        // 3. Populate Target Fields
        if (!isExtendable && targetField != null) {
            if (targetField == UpdateField.EXPECTED) {
                if (dto.getExpected() != null) expectedField.setText(dto.getExpected());
                contentPanel.add(expectedWrapper);

            } else if (targetField == UpdateField.PRIORITY) {
                contentPanel.add(priorityWrapper);

            } else if (targetField == UpdateField.GROUPS) {
                addGroups(groupsPanel, dto.getGroups());
                contentPanel.add(groupsWrapper);

            } else if (targetField == UpdateField.STEPS) {
                contentPanel.add(stepsWrapper);

                if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
                    for (String step : dto.getSteps())
                        addStepField(project, stepsContainer, stepFields, step, repackPopup, uniqueStepsCache);
                } else {
                    addStepField(project, stepsContainer, stepFields, "", repackPopup, uniqueStepsCache);
                }
            }
        }

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 4. Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        statusBar.setOpaque(true);
        statusBar.setBackground(UIUtil.getPanelBackground());

        JLabel shortcutLabel = getLabel(isExtendable, targetField);
        //shortcutLabel.setForeground(UIUtil.getLabelForeground());
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        shortcutLabel.setForeground(JBColor.GRAY);
        statusBar.add(shortcutLabel, BorderLayout.WEST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        // 5. Resolve initial focus
        JComponent initialFocus = null;
        if (isExtendable || targetField == UpdateField.TITLE)
            initialFocus = titleField;

        else if (targetField == UpdateField.EXPECTED)
            initialFocus = expectedField;

        else if (targetField == UpdateField.PRIORITY)
            initialFocus = priorityCombo;

        else if (targetField == UpdateField.GROUPS && groupsPanel.getComponentCount() > 0)
            initialFocus = (JComponent) groupsPanel.getComponent(0);

        else if (targetField == UpdateField.STEPS && !stepFields.isEmpty())
            initialFocus = stepFields.getLast();

        // 6. Build Popup
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, initialFocus)
                .setTitle(isExtendable ? "Create Test Case" : "Edit " + Objects.requireNonNull(targetField).getLabel())
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // 7. Delegate Save Logic
        Runnable saveAction = SingleEditorSaveManager.createSaveAction(null, dto, onSave, popupWrapper);
        //Runnable saveAction = SingleEditorSaveManager.createSaveAction(this, dto, onSave, popupWrapper);

        // 8. Delegate Shortcuts
        SingleEditorShortcutManager.registerShortcuts(
                project,
                uniqueStepsCache,
                mainPanel,
                contentPanel,
                isExtendable,
                targetField,
                repackPopup,
                expectedWrapper,
                expectedField,
                priorityWrapper,
                priorityCombo,
                groupsWrapper,
                stepsWrapper,
                stepsContainer,
                stepFields,
                saveAction
        );

        // 9. Show popup
        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }

    // 🌟 تم إصلاح هذه الدالة لتقرأ النص من KeyboardSet بدلاً من البحث عن الحرف (char)
    private static @NotNull JLabel getLabel(final boolean isExtendable, final UpdateField targetField) {
        String shortcutText;
        if (isExtendable) {
            shortcutText = String.format("💡 [Enter] Save   |   [%s] %s   |   [%s] %s   |   [%s] %s   |   [%s] %s",
                    KeyboardSet.CreateTestCaseExpected.getShortcutText(), UpdateField.EXPECTED.getLabel(),
                    KeyboardSet.CreateTestCaseAddStep.getShortcutText(), UpdateField.STEPS.getLabel(),
                    KeyboardSet.CreateTestCasePriority.getShortcutText(), UpdateField.PRIORITY.getLabel(),
                    KeyboardSet.CreateTestCaseGroups.getShortcutText(), UpdateField.GROUPS.getLabel());

        } else if (targetField == UpdateField.STEPS) {
            shortcutText = String.format("💡 Shortcuts:  [Enter] Save   |   [%s] Add Step   |   [Tab] / [Shift+Tab] Navigate",
                    KeyboardSet.CreateTestCaseAddStep.getShortcutText());

        } else {
            shortcutText = "💡 Shortcuts:  [Enter] Save   |   [Tab] / [Shift+Tab] Navigate";
        }

        return new JLabel(shortcutText);
    }
}