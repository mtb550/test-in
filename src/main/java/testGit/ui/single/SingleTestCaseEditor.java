package testGit.ui.single;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Config;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.bulk.UpdateField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static testGit.ui.single.SingleEditorUIFactory.*;

public class SingleTestCaseEditor {

    public static void showForCreate(Consumer<TestCaseDto> onSave) {
        buildAndShow(new TestCaseDto(), true, null, onSave);
    }

    public static void showForEdit(TestCaseDto existingDto, UpdateField targetField, Consumer<TestCaseDto> onSave) {
        buildAndShow(existingDto, false, targetField, onSave);
    }

    private static void buildAndShow(TestCaseDto dto, boolean isExtendable, UpdateField targetField, Consumer<TestCaseDto> onSave) {
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
        ExtendableTextField titleField;
        if (!isExtendable && targetField != null) {
            if (targetField != UpdateField.TITLE)
                contentPanel.add(createReadOnlyField(dto.getTitle(), UpdateField.TITLE.getIcon(), TITLE_FONT_SIZE));

            if (targetField == UpdateField.STEPS && dto.getExpected() != null && !dto.getExpected().isEmpty())
                contentPanel.add(createReadOnlyField("Expected: " + dto.getExpected(), UpdateField.EXPECTED.getIcon(), FIELD_FONT_SIZE));
        }

        // 2. Editable Fields
        if (isExtendable || targetField == UpdateField.TITLE) {
            titleField = createTextField(UpdateField.TITLE.getLabel(), UpdateField.TITLE.getIcon(), TITLE_FONT_SIZE);
            if (dto.getTitle() != null) titleField.setText(dto.getTitle());
            contentPanel.add(titleField);
        } else {
            titleField = null;
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
        List<ExtendableTextField> stepFields = new ArrayList<>();

        // 3. Populate Target Fields
        if (!isExtendable && targetField != null) {
            if (targetField == UpdateField.EXPECTED) {
                if (dto.getExpected() != null) expectedField.setText(dto.getExpected());
                contentPanel.add(expectedWrapper);
            } else if (targetField == UpdateField.PRIORITY) {
                contentPanel.add(priorityWrapper);
            } else if (targetField == UpdateField.GROUPS) {
                prefillGroups(groupsPanel, dto.getGroups());
                contentPanel.add(groupsWrapper);
            } else if (targetField == UpdateField.STEPS) {
                contentPanel.add(stepsWrapper);
                if (dto.getSteps() != null && !dto.getSteps().isEmpty()) {
                    for (String step : dto.getSteps()) addStepField(stepsContainer, stepFields, step, repackPopup);
                } else {
                    addStepField(stepsContainer, stepFields, "", repackPopup);
                }
            }
        }

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // 4. Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(JBUI.Borders.empty(6, 10));
        statusBar.setOpaque(true);
        statusBar.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());

        String shortcutText;
        if (isExtendable) {
            shortcutText = String.format("💡 [Enter] Save   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s   |   [Ctrl+%c] %s",
                    UpdateField.EXPECTED.getShortcut(), UpdateField.EXPECTED.getLabel(),
                    UpdateField.STEPS.getShortcut(), UpdateField.STEPS.getLabel(),
                    UpdateField.PRIORITY.getShortcut(), UpdateField.PRIORITY.getLabel(),
                    UpdateField.GROUPS.getShortcut(), UpdateField.GROUPS.getLabel());
        } else if (targetField == UpdateField.STEPS) {
            shortcutText = String.format("💡 Shortcuts:  [Enter] Save   |   [Ctrl+%c] Add Step   |   [Tab] / [Shift+Tab] Navigate", UpdateField.STEPS.getShortcut());
        } else {
            shortcutText = "💡 Shortcuts:  [Enter] Save   |   [Tab] / [Shift+Tab] Navigate";
        }

        JLabel shortcutLabel = new JLabel(shortcutText);
        shortcutLabel.setForeground(com.intellij.util.ui.UIUtil.getLabelForeground());
        shortcutLabel.setFont(JBUI.Fonts.smallFont());
        shortcutLabel.setForeground(JBColor.GRAY);
        statusBar.add(shortcutLabel, BorderLayout.WEST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        // 5. Build Popup
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, titleField != null ? titleField : expectedField)
                .setTitle(isExtendable ? "Create Test Case" : "Edit " + targetField.getLabel())
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // 6. Shortcuts & Focus
        if (isExtendable) {
            registerShortcut(mainPanel, UpdateField.EXPECTED.getShortcut(), KeyEvent.CTRL_DOWN_MASK, () -> {
                if (expectedWrapper.getParent() == null) contentPanel.add(expectedWrapper);
                repackPopup.run();
                expectedField.requestFocus();
            });
            registerShortcut(mainPanel, UpdateField.PRIORITY.getShortcut(), KeyEvent.CTRL_DOWN_MASK, () -> {
                if (priorityWrapper.getParent() == null) contentPanel.add(priorityWrapper);
                repackPopup.run();
                priorityCombo.requestFocus();
            });
            registerShortcut(mainPanel, UpdateField.GROUPS.getShortcut(), KeyEvent.CTRL_DOWN_MASK, () -> {
                if (groupsWrapper.getParent() == null) contentPanel.add(groupsWrapper);
                repackPopup.run();
            });
            registerShortcut(mainPanel, UpdateField.STEPS.getShortcut(), KeyEvent.CTRL_DOWN_MASK, () -> {
                if (stepsWrapper.getParent() == null) contentPanel.add(stepsWrapper);
                addStepField(stepsContainer, stepFields, "", repackPopup);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });
        } else if (targetField == UpdateField.STEPS) {
            registerShortcut(mainPanel, UpdateField.STEPS.getShortcut(), KeyEvent.CTRL_DOWN_MASK, () -> {
                addStepField(stepsContainer, stepFields, "", repackPopup);
                repackPopup.run();
                stepFields.getLast().requestFocus();
            });
        }

        registerShortcut(mainPanel, KeyEvent.VK_TAB, 0, () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());
        registerShortcut(mainPanel, KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK, () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());

        // 7. Save Action
        Runnable saveAction = () -> {
            if (titleField != null) dto.setTitle(titleField.getText().trim());
            if (expectedWrapper.getParent() != null) dto.setExpected(expectedField.getText().trim());

            if (priorityWrapper.getParent() != null) dto.setPriority((Priority) priorityCombo.getSelectedItem());
            else if (dto.getPriority() == null) dto.setPriority(Priority.LOW);

            if (groupsWrapper.getParent() != null) {
                List<Groups> selectedGroups = new ArrayList<>();
                for (Component c : groupsPanel.getComponents()) {
                    if (c instanceof JBCheckBox checkBox && checkBox.isSelected()) {
                        selectedGroups.add(Groups.valueOf(checkBox.getText()));
                    }
                }
                dto.setGroups(selectedGroups.isEmpty() ? null : selectedGroups);
            }

            if (stepsWrapper.getParent() != null) {
                List<String> finalSteps = new ArrayList<>();
                for (ExtendableTextField sf : stepFields) {
                    if (!sf.getText().trim().isEmpty()) finalSteps.add(sf.getText().trim());
                }
                dto.setSteps(finalSteps.isEmpty() ? null : finalSteps);
            }

            if (titleField == null || !dto.getTitle().isEmpty()) {
                onSave.accept(dto);
                popupWrapper[0].closeOk(null);
            }
        };
        registerShortcut(mainPanel, KeyEvent.VK_ENTER, 0, saveAction);

        // 8. Initial Focus Setup
        SwingUtilities.invokeLater(() -> {
            if (!isExtendable && targetField != null) {
                if (targetField == UpdateField.EXPECTED) expectedField.requestFocus();
                else if (targetField == UpdateField.PRIORITY) priorityCombo.requestFocus();
                else if (targetField == UpdateField.STEPS && !stepFields.isEmpty())
                    stepFields.getFirst().requestFocus();
                else if (titleField != null) titleField.requestFocus();
            } else if (titleField != null) {
                titleField.requestFocus();
            }
        });

        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }
}