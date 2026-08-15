package org.testin.testcase.update;

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
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.UIAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.testcase.create.*;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

public class UpdateTestCaseDialog extends TestCaseBaseDialog {

    private @Nullable JBPopup popup;

    public UpdateTestCaseDialog(final @NotNull Project p, final @NotNull TestCaseDto existingDto, final @NotNull UpdateTestCaseFields selectedItem, final @NotNull Consumer<@NotNull TestCaseDto> onSave) {
        super(p);

        final UIAction repackPopup = () -> {
            // fillData can run this callback before the popup is created.
            if (popup == null) return;
            popup.pack(false, true);

            ApplicationManager.getApplication().invokeLater(() -> {
                final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof JComponent jComp) {
                    jComp.scrollRectToVisible(new Rectangle(0, 0, jComp.getWidth(), jComp.getHeight()));
                }
            });
        };

        final CreateTestCaseSection targetSection = selectedItem.getSectionExtractor().apply(this);

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

        final JBPanel<?> contentPanel = new JBPanel<>();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(12));

        for (final CreateTestCaseSection section : getAllSections()) {
            final JBPanel<?> slot = new JBPanel<>(new BorderLayout());
            slot.setOpaque(false);

            section.fillData(existingDto, repackPopup);

            final boolean isTarget = (section == targetSection);
            section.setEditable(isTarget);

            if (isTarget && section instanceof StepsSection s) {
                if (s.getStepFields().isEmpty()) {
                    s.addStepField("", repackPopup);
                }
            }

            final boolean showAlways = section instanceof DescriptionSection;
            final boolean showIfNotEmpty = section instanceof ExpectedResultSection && !existingDto.getExpectedResult().isEmpty();

            if (showAlways || showIfNotEmpty || isTarget) {
                section.showSection(slot);
                contentPanel.add(slot);
            }

            if (isTarget) {
                section.setupShortcut(mainPanel, slot, this, repackPopup);
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
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        statusBarSection.updateItems(selectedItem.getStatusBarItems());

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBarSection.getPanel(), BorderLayout.SOUTH);

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
                    public void onClosed(final @NotNull LightweightWindowEvent event) {
                        dispose();
                    }
                })
                .createPopup();

        final Runnable saveAction = save(existingDto, onSave, new JBPopup[]{popup});
        final JBPopup dialogPopup = popup;

        registerShortcut(mainPanel, Shortcuts.Enter.getCustomShortcut(), saveAction::run);

        // Escape is bound rather than left to the popup's own cancel key: once an
        // editor popup has been open over the dialog - the spelling corrections,
        // for one - the built-in handler stops seeing the key.
        registerShortcut(mainPanel, Shortcuts.Escape.getCustomShortcut(), dialogPopup::cancel);
    }

    public void show() {
        // Assigned at the end of the constructor; @Nullable only because the
        // repack callback above can fire before that point.
        Objects.requireNonNull(popup, "popup is created in the constructor").showCenteredInCurrentWindow(p);
    }
}
