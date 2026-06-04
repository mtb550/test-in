package org.testin.ui.testCase.update;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.ui.testCase.*;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.CodeGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class UpdateTestCaseUI extends TestCaseUIBase {

    private final Project project;
    private JBPopup popup;

    public UpdateTestCaseUI(final @NotNull Project project, final TestCaseDto existingDto, final UpdateTestCaseFields selectedItem, final BiConsumer<TestCaseDto, CodeGenerator> onSave) {
        super(project, selectedItem.getChangeType());
        this.project = project;

        IUIAction repackPopup = () -> {
            if (popup != null) {
                popup.pack(false, true);

                SwingUtilities.invokeLater(() -> {
                    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (focusOwner instanceof JComponent jComp) {
                        jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                    }
                });
            }
        };

        ICreateTestCaseSection targetSection = selectedItem.getSectionExtractor().apply(this);

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

        for (ICreateTestCaseSection section : getAllSections()) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);

            section.fillData(existingDto, repackPopup);

            boolean isTarget = (section == targetSection);
            section.setEditable(isTarget);

            if (isTarget && section instanceof StepsSection s) {
                if (s.getStepFields().isEmpty()) {
                    s.addStepField("", repackPopup);
                }
            }

            boolean showAlways = section instanceof DescriptionSection;
            boolean showIfNotEmpty = section instanceof ExpectedResultSection && !existingDto.getExpectedResult().isEmpty();

            if (showAlways || showIfNotEmpty || isTarget) {
                section.showSection(slot);
                contentPanel.add(slot);
            }

            if (isTarget) {
                section.setupShortcut(mainPanel, slot, this, repackPopup);
            }
        }

        JPanel anchorPanel = new JPanel(new BorderLayout());
        anchorPanel.setOpaque(false);
        anchorPanel.add(contentPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JBScrollPane(anchorPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        statusBarSection.updateItems(selectedItem.getStatusBarItems());

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBarSection.getPanel(), BorderLayout.SOUTH);

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, targetSection.getFocusComponent())
                .setTitle("Update " + selectedItem.getName())
                .setSettingButtons(codeGenerator)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(false)
                .setResizable(false)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        dispose();
                    }
                })
                .createPopup();

        Runnable saveAction = save(existingDto, onSave, new JBPopup[]{popup});

        registerShortcut(mainPanel, KeyboardSet.Enter.getCustomShortcut(), saveAction::run);
    }

    public void show() {
        if (popup != null) {
            popup.showCenteredInCurrentWindow(project);
        }
    }
}