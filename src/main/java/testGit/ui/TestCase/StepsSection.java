package testGit.ui.TestCase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;
import testGit.util.services.TestCaseCacheService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class StepsSection implements CreateTestCaseSection {
    @Getter
    private final List<TextFieldWithAutoCompletion<String>> stepFields;
    private final JPanel stepsContainer;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    public StepsSection() {
        this.stepFields = new ArrayList<>();

        this.stepsContainer = new JPanel();
        this.stepsContainer.setLayout(new BoxLayout(this.stepsContainer, BoxLayout.Y_AXIS));
        this.stepsContainer.setOpaque(false);

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(createIconPanel(CreateTestCaseFields.STEPS.getIcon()), BorderLayout.WEST);
        this.wrapper.add(this.stepsContainer, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(final JPanel contentPanel) {
        if (wrapper.getParent() == null) {
            contentPanel.add(wrapper);
        }
    }

    public void showSection(final JPanel contentPanel, final TestCaseUIBase.UIAction repackAction) {
        showSection(contentPanel);
        wrapper.setVisible(true);
        addStepField("", repackAction);
        SwingUtilities.invokeLater(() -> {
            repackAction.execute();
            if (!stepFields.isEmpty()) {
                stepFields.getLast().requestFocus();
            }
        });
    }

    public void addStepField(final String text, final TestCaseUIBase.UIAction repackAction) {
        TextFieldWithAutoCompletionListProvider<String> provider = new TextFieldWithAutoCompletion.StringsCompletionProvider(TestCaseCacheService.getInstance(Config.getProject()).getSteps(), CreateTestCaseFields.STEPS.getIcon());
        TextFieldWithAutoCompletion<String> stepField = new TextFieldWithAutoCompletion<>(Config.getProject(), provider, false, text != null ? text : "");

        stepField.setFont(fieldFont);
        stepField.setPlaceholder("Step " + (stepFields.size() + 1));
        stepField.setShowPlaceholderWhenFocused(true);
        stepField.setBorder(JBUI.Borders.empty(6, 10));

        JPanel stepRow = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        stepRow.setOpaque(false);
        stepRow.setBorder(JBUI.Borders.emptyBottom(6));

        JLabel removeButton = new JLabel(AllIcons.Actions.Cancel);
        removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove step " + KeyboardSet.CreateTestCaseRemoveStep.getShortcutText());

        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                removeButton.setIcon(AllIcons.General.Remove);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeButton.setIcon(AllIcons.Actions.Cancel);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                removeStepAction(stepRow, stepField, repackAction);
            }
        });

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (SwingUtilities.isDescendingFrom(focusOwner, stepField)) {
                    removeStepAction(stepRow, stepField, repackAction);
                }
            }
        }.registerCustomShortcutSet(KeyboardSet.CreateTestCaseRemoveStep.getCustomShortcut(), stepField);

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.setBorder(JBUI.Borders.emptyRight(4));
        buttonWrapper.add(removeButton, BorderLayout.CENTER);

        stepRow.add(stepField, BorderLayout.CENTER);
        stepRow.add(buttonWrapper, BorderLayout.EAST);

        stepFields.add(stepField);
        stepsContainer.add(stepRow);
    }

    private void removeStepAction(JPanel stepRow, TextFieldWithAutoCompletion<String> stepField, TestCaseUIBase.UIAction repackAction) {
        if (stepFields.size() == 1) {
            stepField.setText("");
            stepField.requestFocus();
            return;
        }

        stepsContainer.remove(stepRow);
        stepFields.remove(stepField);

        for (int i = 0; i < stepFields.size(); i++)
            stepFields.get(i).setPlaceholder("Step " + (i + 1));

        if (!stepFields.isEmpty())
            stepFields.getLast().requestFocus();
        SwingUtilities.invokeLater(repackAction::execute);
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            List<String> finalSteps = new ArrayList<>();
            for (TextFieldWithAutoCompletion<String> sf : stepFields) {
                if (!sf.getText().trim().isEmpty()) {
                    finalSteps.add(sf.getText().trim());
                }
            }
            dto.setSteps(finalSteps);
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final TestCaseUIBase base, final TestCaseUIBase.UIAction repackAction) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseAddStep.getCustomShortcut(), () ->
                showSection(slot, repackAction));
    }

    @Override
    public JComponent getFocusComponent() {
        if (!stepFields.isEmpty()) {
            return stepFields.getLast();
        }
        return stepsContainer;
    }

    @Override
    public void setEditable(final boolean editable) {
        for (TextFieldWithAutoCompletion<String> field : stepFields) {
            field.setEnabled(editable);
            Container row = field.getParent();
            if (row != null) {
                for (Component c : row.getComponents()) {
                    if (c instanceof JPanel buttonWrapper) {
                        buttonWrapper.setVisible(editable);
                    }
                }
            }
        }
    }

    public void setStepsData(List<String> steps, TestCaseUIBase.UIAction repack) {
        stepsContainer.removeAll();
        stepFields.clear();
        if (steps != null && !steps.isEmpty()) {
            for (String step : steps) {
                addStepField(step, repack);
            }
        }
    }

    @Override
    public void fillData(final TestCaseDto dto, final TestCaseUIBase.UIAction repackAction) {
        setStepsData(dto.getSteps(), repackAction);
    }
}