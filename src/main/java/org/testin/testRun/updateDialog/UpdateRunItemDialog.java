package org.testin.testRun.updateDialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.RunItemUpdateFields;
import org.testin.mappers.TestRunItems;
import org.testin.testRun.createDialog.ActualResultSection;
import org.testin.testRun.createDialog.BugPrioritySection;
import org.testin.testRun.createDialog.BugSeveritySection;
import org.testin.testRun.createDialog.ErrorCaptureSection;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class UpdateRunItemDialog {

    @Getter
    @NotNull
    private final ActualResultSection actualResultSection;

    @Getter
    @NotNull
    private final BugPrioritySection bugPrioritySection;

    @Getter
    @NotNull
    private final BugSeveritySection bugSeveritySection;

    @Getter
    @NotNull
    private final ErrorCaptureSection errorCaptureSection;

    private final @NotNull Project p;
    private final TestRunItems runItem;
    private final RunItemUpdateFields selectedItem;
    private final Consumer<TestRunItems> onSave;
    private final JBPopup popup;
    private final List<RunItemEditSection> cachedSections;

    public UpdateRunItemDialog(final @NotNull Project p, final @NotNull TestRunItems runItem, final @NotNull RunItemUpdateFields selectedItem, final @NotNull Consumer<TestRunItems> onSave) {
        this.p = p;
        this.runItem = runItem;
        this.selectedItem = selectedItem;
        this.onSave = onSave;

        this.actualResultSection = new ActualResultSection();
        this.bugPrioritySection = new BugPrioritySection();
        this.bugSeveritySection = new BugSeveritySection();
        this.errorCaptureSection = new ErrorCaptureSection();

        this.cachedSections = Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .map(field -> field.getSectionExtractor().create(this))
                .toList();

        List<RunItemEditSection> allSections = getCachedSections();
        RunItemEditSection targetSection = selectedItem.getSectionExtractor().create(this);

        allSections.forEach(s -> s.fillData(runItem));

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

        for (RunItemEditSection section : allSections) {
            JBPanel<?> slot = new JBPanel<>(new BorderLayout());
            slot.setOpaque(false);

            boolean isTarget = (section == targetSection);

            if (isTarget) {
                section.showSection(slot);
                contentPanel.add(slot);
            }
        }

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
                .createComponentPopupBuilder(mainPanel, targetSection.getFocusComponent())
                .setTitle("Update " + selectedItem.getName())
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(false)
                .setMovable(false)
                .setResizable(false)
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        // Save only on OK; Escape/cancel must not commit, and the Enter
                        // shortcut must not save twice.
                        if (event.isOk()) {
                            applyChanges();
                            onSave.accept(runItem);
                        }
                    }
                })
                .createPopup();

        registerEnterShortcut(mainPanel);
    }

    private List<RunItemEditSection> getCachedSections() {
        return cachedSections;
    }

    private void applyChanges() {
        getCachedSections().forEach(s -> s.applyTo(runItem));
    }

    private void registerEnterShortcut(final JComponent component) {
        com.intellij.openapi.project.DumbAwareAction saveAction = new com.intellij.openapi.project.DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                // closeOk fires onClosed with isOk() == true, which applies and saves once.
                if (popup != null) {
                    popup.closeOk(null);
                }
            }

            @Override
            public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
                return com.intellij.openapi.actionSystem.ActionUpdateThread.EDT;
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
