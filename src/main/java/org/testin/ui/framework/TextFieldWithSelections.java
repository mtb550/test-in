package org.testin.ui.framework;

import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A large input field over a selection list — the component of the create
 * dialogs. The leading icon of the field follows the selected row, a mouse
 * click on a row submits, and Up/Down move the selection while focus stays in
 * the field. Rendering matches the existing create-node popup exactly.
 */
public final class TextFieldWithSelections<T> implements DialogComponent {

    private final @NotNull ExtendableTextField textField;
    private final @NotNull JBList<SelectionList<T>> list;
    private final @NotNull JBPanel<?> panel;
    private final @Nullable String placeHolderText;
    private @NotNull Runnable submitRequest = () -> {
    };
    private boolean emptyWarningShown;

    TextFieldWithSelections(final @Nullable Icon icon,
                            final @Nullable String placeHolderText,
                            final @NotNull List<SelectionList<T>> selections) {
        this.placeHolderText = placeHolderText;
        textField = new ExtendableTextField("");
        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        textField.setFont(JBFont.label().biggerOn(6f));
        // 12px left rhythm shared by the field text and the list rows below.
        textField.setBorder(JBUI.Borders.empty(10, 12));

        if (placeHolderText != null && !placeHolderText.isBlank()) {
            textField.getEmptyText().setText(placeHolderText);
            TextComponentEmptyText.setupPlaceholderVisibility(textField);
            // Typing clears a red empty-submit warning back to the normal look.
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(final @NotNull DocumentEvent e) {
                    if (emptyWarningShown) {
                        emptyWarningShown = false;
                        showPlaceholder(SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
            });
        }
        DialogStyle.setLeadingIcon(textField, icon);

        list = new JBList<>(selections);
        list.setBorder(JBUI.Borders.empty(6));
        list.setFont(JBFont.label().biggerOn(2f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(selections.size());
        list.setCellRenderer(new SelectionRenderer<>());

        list.addListSelectionListener(event -> syncLeadingIcon());
        syncLeadingIcon();

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                if (list.locationToIndex(event.getPoint()) >= 0) submitRequest.run();
            }
        });

        installNavigation();

        final JBPanel<?> listWrapper = new JBPanel<>(new BorderLayout());
        listWrapper.add(list, BorderLayout.CENTER);

        final JBScrollPane scrollPane = new JBScrollPane(listWrapper);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(textField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    public @NotNull String getText() {
        return textField.getText();
    }

    /**
     * Turns the placeholder red until the tester types — the empty-submit cue.
     */
    public void showEmptyWarning() {
        emptyWarningShown = true;
        showPlaceholder(SimpleTextAttributes.ERROR_ATTRIBUTES);
        textField.requestFocusInWindow();
    }

    private void showPlaceholder(final @NotNull SimpleTextAttributes attributes) {
        if (placeHolderText == null || placeHolderText.isBlank()) return;

        textField.getEmptyText().clear();
        textField.getEmptyText().appendText(placeHolderText, attributes);
        textField.repaint();
    }

    public @NotNull T getSelectedValue() {
        // A single-selection list can still be emptied (e.g. Ctrl+click on the
        // selected row); the first row is the declared default.
        final SelectionList<T> selected = list.getSelectedValue();
        return (selected != null ? selected : list.getModel().getElementAt(0)).value();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return textField;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        this.submitRequest = submit;
    }

    private void syncLeadingIcon() {
        final SelectionList<T> selected = list.getSelectedValue();
        if (selected == null) return;

        // setExtensions does not refresh the field on its own; without this
        // the icon goes stale when the selection moves by keyboard.
        DialogStyle.setLeadingIcon(textField, selected.icon());
        textField.revalidate();
        textField.repaint();
    }

    private void installNavigation() {
        bindNavigationKey(KeyEvent.VK_DOWN, "testin.framework.selectionDown", 1);
        bindNavigationKey(KeyEvent.VK_UP, "testin.framework.selectionUp", -1);
    }

    private void bindNavigationKey(final int keyCode, final @NotNull String actionKey, final int delta) {
        textField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
        textField.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                moveSelectionBy(delta);
            }
        });
    }

    private void moveSelectionBy(final int delta) {
        final int size = list.getModel().getSize();
        if (size == 0) return;

        final int newIdx = Math.clamp(list.getSelectedIndex() + delta, 0, size - 1);
        list.setSelectedIndex(newIdx);
        list.ensureIndexIsVisible(newIdx);
    }

    private static final class SelectionRenderer<T> extends ColoredListCellRenderer<SelectionList<T>> {
        @Override
        protected void customizeCellRenderer(final @NotNull JList<? extends SelectionList<T>> list, final SelectionList<T> value,
                                             final int index, final boolean selected, final boolean hasFocus) {
            setIcon(value.icon());
            setIconTextGap(JBUI.scale(8));
            append(value.name());
            if (value.hint() != null) {
                // Platform hint standard: theme-aware gray, italic.
                append("   " + value.hint(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
            }
            setBorder(JBUI.Borders.empty(8, 12));
        }
    }
}
