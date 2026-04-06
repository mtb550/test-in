package testGit.ui.createTestCase;

import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;

public class CreateTestCaseUI extends CreateTestCaseBase {

    public void show(final Consumer<TestCaseDto> onSave, final Set<String> uniqueStepsCache) {
        TestCaseDto dto = new TestCaseDto();
        final JBPopup[] popupWrapper = new JBPopup[1];

        UIAction repackPopup = () -> {
            if (popupWrapper[0] != null) popupWrapper[0].pack(false, true);
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
            contentPanel.add(slot);

            section.setupShortcut(mainPanel, slot, this, repackPopup, uniqueStepsCache);

            if (section instanceof TitleSection)
                section.showSection(slot);
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
        mainPanel.add(statusBar.getPanel(), BorderLayout.SOUTH);

        // Popup
        popupWrapper[0] = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, titleSection.getFocusComponent())
                .setTitle("Create Test Case")
                //.setTitleIcon()
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .setResizable(true)
                .createPopup();

        // save
        Runnable saveAction = save(dto, onSave, popupWrapper);

        // registe enter shortcut
        registerShortcut(mainPanel, KeyboardSet.Enter.getShortcut(), saveAction::run);

        // show first
        popupWrapper[0].showCenteredInCurrentWindow(Config.getProject());
    }

}