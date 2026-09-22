/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;
import org.testin.util.ListValue;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class TextFieldWithSelections<T> implements DialogComponent, TextValue {
    private static final int DEBOUNCE_MILLIS = 300;

    private final @NotNull FrameworkTextField input;
    private final @NotNull JTextField textField;
    private final @NotNull CollectionListModel<SelectionList<T>> rowModel = new CollectionListModel<>();
    private final @NotNull JBList<SelectionList<T>> list;
    private final @NotNull JBPanel<?> panel;
    private final @NotNull Rows<T> rows;
    private final @NotNull AtomicInteger queryGeneration = new AtomicInteger();
    private @NotNull Runnable submitRequest = () -> {
    };
    private boolean askedOnce;

    TextFieldWithSelections(final @NotNull Icon icon, final @NotNull String placeHolderText, final @NotNull List<SelectionList<T>> shownBeforeAsking, final @NotNull Rows<T> rows, final int visibleRows) {
        this.rows = rows;
        input = new FrameworkTextField(icon, placeHolderText, "");
        textField = input.component();

        list = new JBList<>(rowModel);
        list.setBorder(JBUI.Borders.empty(6));
        DialogStyle.asChoice(list);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(visibleRows);
        list.setCellRenderer(new SelectionRenderer<>());
        show(shownBeforeAsking);

        list.addListSelectionListener(_ -> syncLeadingIcon());
        syncLeadingIcon();

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                if (list.locationToIndex(event.getPoint()) >= 0) submitRequest.run();
            }
        });

        installNavigation();

        final @NotNull JBPanel<?> listWrapper = new JBPanel<>(new BorderLayout());
        listWrapper.add(list, BorderLayout.CENTER);

        final @NotNull JBScrollPane scrollPane = new JBScrollPane(listWrapper);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(textField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        installRowRefresh();
        installFirstFill();
    }

    private void installFirstFill() {
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (askedOnce || !panel.isShowing()) return;

            askedOnce = true;
            requestRows();
        });
    }

    private void installRowRefresh() {
        final @NotNull Timer debounce = new Timer(DEBOUNCE_MILLIS, _ -> requestRows());
        debounce.setRepeats(false);

        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                debounce.restart();
            }
        });
    }

    private void requestRows() {
        if (!panel.isShowing()) return;

        final int generation = queryGeneration.incrementAndGet();
        final @NotNull String query = textField.getText().trim();

        final @NotNull ModalityState modality = ModalityState.stateForComponent(panel);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Rows.Answer<T> answer = rows.forQuery(query);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != queryGeneration.get() || !panel.isShowing()) return;

                show(answer.rows());
                input.setNote(answer.note());
            }, modality);
        });
    }

    private void show(final @NotNull List<SelectionList<T>> found) {
        if (found.equals(rowModel.getItems())) return;

        rowModel.replaceAll(found);

        if (found.isEmpty()) return;

        list.setSelectedIndex(0);
        list.ensureIndexIsVisible(0);
    }

    public @NotNull Optional<T> selection() {
        return ListValue.selected(list).map(SelectionList::value);
    }

    @Override
    public @NotNull String getText() {
        return textField.getText();
    }

    @Override
    public void showEmptyWarning() {
        input.showEmptyWarning();
    }

    public @NotNull T getSelectedValue() {
        return ListValue.selected(list).orElseGet(() -> list.getModel().getElementAt(0)).value();
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
        ListValue.selected(list).ifPresent(selected -> {
            input.setLeadingIcon(selected.icon());
            textField.revalidate();
            textField.repaint();
        });
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
        protected void customizeCellRenderer(final @NotNull JList<? extends SelectionList<T>> list, final SelectionList<T> value, final int index, final boolean selected, final boolean hasFocus) {
            setIcon(value.icon());
            setIconTextGap(JBUI.scale(8));
            append(value.name());
            if (!value.hint().isEmpty()) {
                append("   " + value.hint(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
            }
            setBorder(JBUI.Borders.empty(8, 12));
        }
    }
}
