package testGit.ui.TestCase.edit;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.ui.TestCase.*;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class EditTestCaseUI extends TestCaseUIBase {

    public void show(final TestCaseDto existingDto, final EditField targetField, final Consumer<TestCaseDto> updatedItems) {
        final JBPopup[] popupWrapper = new JBPopup[1];
        UIAction repackPopup = () -> {
            if (popupWrapper[0] != null)
                popupWrapper[0].pack(false, true);
        };

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension pref = super.getPreferredSize();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                pref.width = Math.max(pref.width, screenSize.width / 2);
                int maxHeight = (int) (screenSize.height * 0.85);
                pref.height = Math.min(pref.height, maxHeight);
                return pref;
            }
        };

        mainPanel.setBorder(JBUI.Borders.empty());
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        for (CreateTestCaseSection section : getAllSections()) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);

            section.fillData(existingDto, repackPopup);

            boolean isTarget = isTargetSection(section, targetField);
            section.setEditable(isTarget);

            if (isTarget && section instanceof StepsSection s) {
                if (s.getStepFields().isEmpty()) {
                    s.addStepField("", repackPopup);
                }
            }

            boolean showAlways = section instanceof TitleSection;
            boolean showIfNotEmpty = section instanceof ExpectedSection && existingDto.getExpected() != null && !existingDto.getExpected().isEmpty();

            if (showAlways || showIfNotEmpty || isTarget) {
                section.showSection(slot);
                contentPanel.add(slot);
            }

            if (isTarget) {
                section.setupShortcut(mainPanel, slot, this, repackPopup);
            }
        }

        setupUI(mainPanel, contentPanel, popupWrapper, existingDto, targetField, updatedItems);
    }

    private boolean isTargetSection(final CreateTestCaseSection section, final EditField target) {
        return switch (target) {
            case TITLE -> section instanceof TitleSection;
            case EXPECTED -> section instanceof ExpectedSection;
            case PRIORITY -> section instanceof PrioritySection;
            case GROUPS -> section instanceof GroupsSection;
            case STEPS -> section instanceof StepsSection;
            default -> false;
        };
    }

    private JComponent getTargetFocus(final EditField target) {
        return getAllSections().stream()
                .filter(section -> isTargetSection(section, target))
                .map(CreateTestCaseSection::getFocusComponent)
                .findFirst()
                .orElse(titleSection.getFocusComponent());
    }

    private void setupUI(final JPanel mainPanel, final JPanel contentPanel, final JBPopup[] popupWrapper, final TestCaseDto dto, final EditField target, final Consumer<TestCaseDto> updatedItems) {
        JPanel anchorPanel = new JPanel(new BorderLayout());
        anchorPanel.setOpaque(false);
        anchorPanel.add(contentPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JBScrollPane(anchorPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        statusBar.updateItems(target.getStatusBarItems());

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar.getPanel(), BorderLayout.SOUTH);

        // Popup
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, getTargetFocus(target))
                .setTitle("Edit " + target.getLabel())
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // save
        Runnable saveAction = save(dto, updatedItems, popupWrapper);

        // register enter shortcut
        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction::run);

        // show first
        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }
}