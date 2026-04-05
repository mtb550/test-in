package testGit.ui.single.nnew;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.single.SingleEditorSaveManager;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
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
        titleSection.showSection(contentPanel); // build first
        ExtendableTextField titleField = titleSection.getTitleField();
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseTitle.getShortcut(), () -> {
            titleSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 2. Expected
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getShortcut(), () -> {
            expectedSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 3. Priority
        registerShortcut(mainPanel, KeyboardSet.CreateTestCasePriority.getShortcut(), () -> {
            prioritySection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 4. Groups
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseGroups.getShortcut(), () -> {
            groupsSection.showSection(contentPanel);
            repackPopup.execute();
        });

        // 5. Steps
        registerShortcut(mainPanel, KeyboardSet.CreateTestCaseAddStep.getShortcut(), () ->
                stepsSection.showSection(contentPanel, repackPopup, uniqueStepsCache));

        // status bar
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(statusBarSection.getPanel(), BorderLayout.SOUTH);

        // Popup
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, titleField)
                .setTitle("Create Test Case")
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // save
        Runnable saveAction = SingleEditorSaveManager.createSaveAction(this, dto, onSave, popupWrapper);

        // General Shortcuts
        registerShortcut(mainPanel, KeyboardSet.TabNext.getShortcut(), () ->
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent());

        registerShortcut(mainPanel, KeyboardSet.TabPrevious.getShortcut(), () ->
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent());

        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction::run);

        // show first
        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }

}