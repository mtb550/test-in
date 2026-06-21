package org.testin.ui.testRun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;

public class ActualResultUI {

    private final Project project;
    private final TestRunItems runItem;
    private final ActualResultSection section;
    private final JBPopup popup;

    public ActualResultUI(final @NotNull Project project, final @NotNull TestRunItems runItem) {
        this.project = project;
        this.runItem = runItem;
        this.section = new ActualResultSection();

        section.fillData(runItem);

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

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(section.getWrapper(), BorderLayout.NORTH);

        JBScrollPane scrollPane = new JBScrollPane(contentPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, section.getActualResultField())
                .setTitle("Set Actual Result")
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(true)
                .setResizable(true)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        // apply changes on close
                        section.applyTo(runItem);
                    }
                })
                .createPopup();

        // Register Enter shortcut to save and close
        registerEnterShortcut(mainPanel);
    }

    private void registerEnterShortcut(final JComponent component) {
        DumbAwareAction saveAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (popup != null) {
                    section.applyTo(runItem);
                    popup.closeOk(null);
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        saveAction.registerCustomShortcutSet(KeyboardSet.Enter.getCustomShortcut(), component);
    }

    public void show() {
        if (popup != null) {
            popup.showCenteredInCurrentWindow(project);
        }
    }
}
