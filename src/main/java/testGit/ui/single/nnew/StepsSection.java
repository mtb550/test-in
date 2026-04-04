package testGit.ui.single.nnew;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.TextFieldWithAutoCompletionListProvider;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import testGit.pojo.Config;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class StepsSection {
    private final JPanel stepsContainer;
    private final JPanel wrapperPanel;
    @Getter
    private final List<TextFieldWithAutoCompletion<String>> stepFields;
    private final float fontSize = JBUI.Fonts.label().getSize2D() + 2f;

    public StepsSection() {
        this.stepFields = new ArrayList<>();

        // 1. حاوية الخطوات (التي ستُرتب العناصر فوق بعضها)
        this.stepsContainer = new JPanel();
        this.stepsContainer.setLayout(new BoxLayout(this.stepsContainer, BoxLayout.Y_AXIS));
        this.stepsContainer.setOpaque(false);

        // 2. تغليف الحاوية (بدون أيقونة جانبية لأن الخطوات ليس لها أيقونة رئيسية في الخارج)
        this.wrapperPanel = new JPanel(new BorderLayout());
        this.wrapperPanel.setOpaque(false);
        this.wrapperPanel.add(this.stepsContainer, BorderLayout.CENTER);
        this.wrapperPanel.setBorder(JBUI.Borders.emptyTop(8));
    }

    // إجراء الإظهار (يضيف خطوة فارغة افتراضياً عند فتح القسم)
    public Runnable getShowAction(JPanel contentPanel, Runnable repackPopup, Set<String> uniqueStepsCache) {
        return () -> {
            if (wrapperPanel.getParent() == null) {
                contentPanel.add(wrapperPanel);
            }
            addStepField("", repackPopup, uniqueStepsCache);
            repackPopup.run();
            stepFields.getLast().requestFocus();
        };
    }

    // تم نقل هذه الدالة من BaseCreateTestCase إلى هنا
    public void addStepField(final String text, final Runnable repackAction, final Set<String> uniqueStepsCache) {
        Project project = Config.getProject();
        TextFieldWithAutoCompletionListProvider<String> provider = new TextFieldWithAutoCompletion.StringsCompletionProvider(uniqueStepsCache != null ? uniqueStepsCache : Collections.emptySet(), null);
        TextFieldWithAutoCompletion<String> stepField = new TextFieldWithAutoCompletion<>(project, provider, true, text != null ? text : "");
        stepField.setFont(JBFont.regular().deriveFont(fontSize));
        stepField.setPlaceholder("Step " + (stepFields.size() + 1));
        stepField.setShowPlaceholderWhenFocused(true);
        stepField.setBorder(JBUI.Borders.empty(6, 10));

        JPanel stepRow = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        stepRow.setOpaque(false);
        stepRow.setBorder(JBUI.Borders.emptyBottom(6));

        JLabel removeButton = new JLabel(AllIcons.Actions.Cancel);
        removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeButton.setToolTipText("Remove step");

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
                stepsContainer.remove(stepRow);
                stepFields.remove(stepField);

                for (int i = 0; i < stepFields.size(); i++) {
                    stepFields.get(i).setPlaceholder("Step " + (i + 1));
                }

                stepsContainer.revalidate();
                stepsContainer.repaint();
                repackAction.run();
            }
        });

        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setOpaque(false);
        buttonWrapper.setBorder(JBUI.Borders.emptyRight(4));
        buttonWrapper.add(removeButton, BorderLayout.CENTER);

        stepRow.add(stepField, BorderLayout.CENTER);
        stepRow.add(buttonWrapper, BorderLayout.EAST);

        stepFields.add(stepField);
        stepsContainer.add(stepRow);
    }

    public JPanel getWrapper() {
        return wrapperPanel;
    }

}