package org.testin.testRun.createDialog;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.TestRunItems;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;

public class ActualResultDialog {

    private final @NotNull Project p;
    private final TestRunItems runItem;
    private final ActualResultSection section;
    private final JBPopup popup;

    public ActualResultDialog(final @NotNull Project p, final @NotNull TestRunItems runItem) {
        this.p = p;
        this.runItem = runItem;
        this.section = new ActualResultSection();

        section.fillData(runItem);

        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout()) {
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
        DialogStyle.styleContent(mainPanel);
        mainPanel.setFocusCycleRoot(true);
        mainPanel.setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

        JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        JBPanel<?> slot = new JBPanel<>(new BorderLayout());
        slot.setOpaque(false);
        section.showSection(slot);
        contentPanel.add(slot);

        JBPanel<?> anchorPanel = new JBPanel<>(new BorderLayout());
        anchorPanel.setOpaque(false);
        anchorPanel.add(contentPanel, BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(anchorPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, section.getFocusComponent())
                .setTitle("Set Actual Result")
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(false)
                .setResizable(false)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        // Commit only on OK; Escape/cancel must not apply the edit.
                        if (event.isOk()) {
                            section.applyTo(runItem);
                        }
                    }
                })
                .createPopup();

        // Register Enter shortcut
        registerEnterShortcut(mainPanel);
    }

    private void registerEnterShortcut(final JComponent component) {
        DumbAwareAction saveAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // closeOk fires onClosed with isOk() == true, which applies once.
                if (popup != null) {
                    popup.closeOk(null);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        saveAction.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), component);
    }

    public void show() {
        if (popup != null) {
            popup.showCenteredInCurrentWindow(p);
        }
    }
}
