package org.testin.testcase.create;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.UIAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class CreateTestCaseDialog extends TestCaseBaseDialog {

    private @Nullable JBPopup popup;

    public CreateTestCaseDialog(final @NotNull Project p, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p);

        final TestCaseDto dto = new TestCaseDto();

        final UIAction repackPopup = () -> {
            if (popup != null) {
                popup.pack(false, true);

                ApplicationManager.getApplication().invokeLater(() -> {
                    final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                    if (focusOwner instanceof JComponent jComp) {
                        jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                    }
                });
            }
        };

        final JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout()) {
            @Override
            public @NotNull Dimension getPreferredSize() {
                final Dimension pref = super.getPreferredSize();
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                pref.width = Math.max(pref.width, screenSize.width / 2);
                final int maxHeight = (int) (screenSize.height * 0.85);
                pref.height = Math.min(pref.height, maxHeight);
                return pref;
            }
        };

        mainPanel.setBorder(JBUI.Borders.empty());
        DialogStyle.styleContent(mainPanel);
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        initDynamicStatusBar(mainPanel);

        final JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        for (final CreateTestCaseSection section : getAllSections()) {
            final JBPanel<?> slot = new JBPanel<>(new BorderLayout());
            slot.setOpaque(false);
            contentPanel.add(slot);

            section.setupShortcut(mainPanel, slot, this, repackPopup);

            if (section instanceof DescriptionSection) {
                section.showSection(slot);
            }
        }

        final JBPanel<?> anchorPanel = new JBPanel<>(new BorderLayout());
        anchorPanel.setOpaque(false);
        anchorPanel.add(contentPanel, BorderLayout.NORTH);

        final JBScrollPane scrollPane = new JBScrollPane(anchorPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(final java.awt.event.ComponentEvent e) {
                final int viewHeight = anchorPanel.getPreferredSize().height;
                final int portHeight = scrollPane.getViewport().getHeight();

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
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(true)
                .setResizable(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(final @NotNull LightweightWindowEvent event) {
                        dispose();
                    }
                })
                .createPopup();

        final Runnable saveAction = save(dto, onSave, new JBPopup[]{popup});
        final JBPopup dialogPopup = popup;

        // register enter shortcut
        registerShortcut(mainPanel, Shortcuts.Enter.getCustomShortcut(), saveAction::run);

        // Escape is bound rather than left to the popup's own cancel key: once an
        // editor popup has been open over the dialog - the spelling corrections,
        // for one - the built-in handler stops seeing the key.
        registerShortcut(mainPanel, Shortcuts.Escape.getCustomShortcut(), dialogPopup::cancel);
    }

    public void show() {
        if (popup != null) {
            popup.showCenteredInCurrentWindow(p);
        }
    }
}
