package org.testin.ui.testCase;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.CodeGenerator;
import org.testin.util.autoGenerator.GeneratorType;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public class CreateTestCaseUI extends TestCaseUIBase {

    private final Project project;
    private JBPopup popup;

    public CreateTestCaseUI(final @NotNull Project project, final BiConsumer<TestCaseDto, CodeGenerator> onSave) {
        super(project, GeneratorType.CREATE_TEST_METHOD);
        this.project = project;

        final TestCaseDto dto = new TestCaseDto();

        IUIAction repackPopup = () -> {
            if (popup != null) {
                popup.pack(false, true);

                // make focus on the new component if scroll pane appear.
                SwingUtilities.invokeLater(() -> {
                    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (focusOwner instanceof JComponent jComp) {
                        jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                    }
                });
            }
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

        initDynamicStatusBar(mainPanel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        for (ICreateTestCaseSection section : getAllSections()) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);
            contentPanel.add(slot);

            section.setupShortcut(mainPanel, slot, this, repackPopup);

            if (section instanceof DescriptionSection) {
                section.showSection(slot);
            }
        }

        JPanel anchorPanel = new JPanel(new BorderLayout());
        anchorPanel.setOpaque(false);
        anchorPanel.add(contentPanel, BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(anchorPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int viewHeight = anchorPanel.getPreferredSize().height;
                int portHeight = scrollPane.getViewport().getHeight();

                if (viewHeight > portHeight) {
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                } else {
                    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                }
            }
        });

        // status bar
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBarSection.getPanel(), BorderLayout.SOUTH);

        // Popup creation
        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, DescriptionSection.getFocusComponent())
                .setTitle("Create Test Case")
                .setSettingButtons(codeGenerator)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(true)
                .setResizable(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        dispose();
                    }
                })
                .createPopup();

        Runnable saveAction = save(dto, onSave, new JBPopup[]{popup});

        // register enter shortcut
        registerShortcut(mainPanel, KeyboardSet.Enter.getCustomShortcut(), saveAction::run);
    }

    public void show() {
        if (popup != null) {
            popup.showCenteredInCurrentWindow(project);
        }
    }
}