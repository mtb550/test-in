package testGit.ui.createTestCase;

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        JLabel iconLabel = new JLabel(CreateField.STEPS.getIcon());
        iconLabel.setBorder(JBUI.Borders.empty(6, 10, 0, 8));
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.add(iconLabel, BorderLayout.CENTER);

        this.wrapper.add(iconPanel, BorderLayout.WEST);
        this.wrapper.add(this.stepsContainer, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
    }

    public void showSection(final JPanel contentPanel, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache) {
        showSection(contentPanel);
        wrapper.setVisible(true);
        addStepField("", repackAction, uniqueStepsCache);
        SwingUtilities.invokeLater(() -> {
            repackAction.execute();
            stepFields.getLast().requestFocus();
        });
    }

    public void addStepField(final String text, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache) {
        TextFieldWithAutoCompletionListProvider<String> provider = new TextFieldWithAutoCompletion.StringsCompletionProvider(uniqueStepsCache != null ? uniqueStepsCache : Collections.emptySet(), null);
        TextFieldWithAutoCompletion<String> stepField = new TextFieldWithAutoCompletion<>(Config.getProject(), provider, true, text != null ? text : "");

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
        }.registerCustomShortcutSet(KeyboardSet.CreateTestCaseRemoveStep.getShortcut(), stepField);

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.setBorder(JBUI.Borders.emptyRight(4));
        buttonWrapper.add(removeButton, BorderLayout.CENTER);

        stepRow.add(stepField, BorderLayout.CENTER);
        stepRow.add(buttonWrapper, BorderLayout.EAST);

        stepFields.add(stepField);
        stepsContainer.add(stepRow);
    }

    private void removeStepAction(JPanel stepRow, TextFieldWithAutoCompletion<String> stepField, CreateTestCaseBase.UIAction repackAction) {
        stepsContainer.remove(stepRow);
        stepFields.remove(stepField);

        for (int i = 0; i < stepFields.size(); i++)
            stepFields.get(i).setPlaceholder("Step " + (i + 1));

        if (stepFields.isEmpty()) wrapper.setVisible(false);
        else stepFields.getLast().requestFocus();

        SwingUtilities.invokeLater(repackAction::execute);
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null && wrapper.isVisible()) {
            List<String> finalSteps = new ArrayList<>();
            for (TextFieldWithAutoCompletion<String> sf : stepFields) {
                if (!sf.getText().trim().isEmpty()) {
                    finalSteps.add(sf.getText().trim());
                }
            }
            dto.setSteps(finalSteps.isEmpty() ? null : finalSteps);
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final CreateTestCaseBase base, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseAddStep.getShortcut(), () ->
                showSection(slot, repackAction, uniqueStepsCache));
    }

    @Override
    public JComponent getFocusComponent() {
        if (!stepFields.isEmpty()) {
            return stepFields.getLast();
        }
        return stepsContainer;
    }
}