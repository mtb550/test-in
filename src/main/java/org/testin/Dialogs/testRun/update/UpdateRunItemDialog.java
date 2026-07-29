package org.testin.Dialogs.testRun.update;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.testRun.*;
import org.testin.pojo.TestRunItems;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class UpdateRunItemDialog {

    @Getter
    private final ActualResultSection actualResultSection;

    @Getter
    private final StatusSection statusSection;

    @Getter
    private final PrioritySection prioritySection;

    @Getter
    private final SeveritySection severitySection;

    @Getter
    private final AttachmentsSection attachmentsSection;

    private final Project project;
    private final TestRunItems runItem;
    private final RunItemUpdateFields selectedItem;
    private final Consumer<TestRunItems> onSave;
    private final JBPopup popup;
    private final List<RunItemEditSection> cachedSections;

    public UpdateRunItemDialog(final @NotNull Project project, final @NotNull TestRunItems runItem, final @NotNull RunItemUpdateFields selectedItem, final @NotNull Consumer<TestRunItems> onSave) {
        this.project = project;
        this.runItem = runItem;
        this.selectedItem = selectedItem;
        this.onSave = onSave;

        this.actualResultSection = new ActualResultSection();
        this.statusSection = new StatusSection();
        this.prioritySection = new PrioritySection();
        this.severitySection = new SeveritySection();
        this.attachmentsSection = new AttachmentsSection();

        this.cachedSections = Arrays.stream(RunItemUpdateFields.values())
                .filter(RunItemUpdateFields::isUpdateMenuItem)
                .map(field -> field.getSectionExtractor().create(this))
                .toList();

        List<RunItemEditSection> allSections = getCachedSections();
        RunItemEditSection targetSection = selectedItem.getSectionExtractor().create(this);

        allSections.forEach(s -> s.fillData(runItem));

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

        for (RunItemEditSection section : allSections) {
            JPanel slot = new JPanel(new BorderLayout());
            slot.setOpaque(false);

            boolean isTarget = (section == targetSection);

            if (isTarget) {
                section.showSection(slot);
                contentPanel.add(slot);
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
                        applyChanges();
                        onSave.accept(runItem);
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
                if (popup != null) {
                    applyChanges();
                    onSave.accept(runItem);
                    popup.closeOk(null);
                }
            }

            @Override
            public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
                return com.intellij.openapi.actionSystem.ActionUpdateThread.EDT;
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
