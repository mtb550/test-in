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
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.ListValue;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A large input field over a selection list — the component of the create
 * dialogs, and of the search (#29). The leading icon of the field follows the
 * selected row, a mouse click on a row submits, and Up/Down move the selection
 * while focus stays in the field. Rendering matches the existing create-node
 * popup exactly.
 * <p>
 * The rows come from {@link Rows}, which is asked again - debounced - every time
 * the tester types. A dialog with fixed choices declares rows that ignore the
 * query, and behaves exactly as it did before that was possible.
 */
public final class TextFieldWithSelections<T> implements DialogComponent {

    /**
     * How long after the last keystroke the rows are asked for again. The same
     * 300ms the editors' own search field waits, so the two feel alike.
     */
    private static final int DEBOUNCE_MILLIS = 300;

    private final @NotNull FrameworkTextField input;
    private final @NotNull ExtendableTextField textField;
    private final @NotNull CollectionListModel<SelectionList<T>> rowModel = new CollectionListModel<>();
    private final @NotNull JBList<SelectionList<T>> list;
    private final @NotNull JBPanel<?> panel;
    private final @NotNull Rows<T> rows;
    /**
     * Which query the rows on screen answer. Bumped on every request so a slow
     * answer arriving after a newer keystroke knows it is stale and steps aside.
     */
    private final @NotNull AtomicInteger queryGeneration = new AtomicInteger();
    private @NotNull Runnable submitRequest = () -> {
    };
    /**
     * Whether the opening rows have been asked for. Once, on the first time the
     * dialog is on screen - a hierarchy event can say "showing" more than once
     * for one dialog, and a search is too expensive to run again for it.
     */
    private boolean askedOnce;

    TextFieldWithSelections(final @NotNull Icon icon, final @NotNull String placeHolderText, final @NotNull List<SelectionList<T>> shownBeforeAsking, final @NotNull Rows<T> rows, final int visibleRows) {
        this.rows = rows;
        input = new FrameworkTextField(icon, placeHolderText, "");
        textField = input.component();

        list = new JBList<>(rowModel);
        list.setBorder(JBUI.Borders.empty(6));
        list.setFont(JBFont.label().biggerOn(2f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Fixed, so the dialog does not change height under the tester's hands
        // as results come and go.
        list.setVisibleRowCount(visibleRows);
        list.setCellRenderer(new SelectionRenderer<>());
        // What there is to show before anything has been asked. A fixed set of
        // choices is all of it and is on screen from here, so Enter works on the
        // first frame; a search has nothing yet and fills in from installFirstFill
        // a moment later, off this thread.
        show(shownBeforeAsking);

        list.addListSelectionListener(event -> syncLeadingIcon());
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
        // As needed rather than never: a fixed list is exactly as tall as its
        // rows and shows no bar, and a search can answer with more rows than fit.
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel = new JBPanel<>(new BorderLayout());
        panel.setOpaque(false);
        panel.add(textField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        installRowRefresh();
        installFirstFill();
    }

    /**
     * Asks for the opening rows once the dialog is on screen, instead of in the
     * constructor.
     * <p>
     * The first list is a query like any other, and for the search it is the
     * broadest one there is - every test set and every test run in the project,
     * copied, filtered and sorted. Building it in the constructor ran that pass
     * on the thread painting the dialog, at the moment the tester pressed the
     * shortcut, which is the one moment they are watching it. A fixed set takes
     * this path too and answers with what is already showing, so nothing moves.
     */
    private void installFirstFill() {
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (askedOnce || !panel.isShowing()) return;

            askedOnce = true;
            requestRows();
        });
    }

    /**
     * Asks for the rows again a moment after the tester stops typing.
     * <p>
     * Debounced because a query of six letters is six queries otherwise, and the
     * last one is the only one anybody sees. The timer does not repeat, and it
     * checks the panel is still on screen before it does anything - a dialog
     * closed within the debounce leaves one pending fire, and it should do
     * nothing rather than rebuild a list nobody is looking at.
     */
    private void installRowRefresh() {
        final @NotNull Timer debounce = new Timer(DEBOUNCE_MILLIS, event -> requestRows());
        debounce.setRepeats(false);

        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NotNull DocumentEvent e) {
                debounce.restart();
            }
        });
    }

    /**
     * Asks {@link Rows} for the query off the EDT, and shows the answer back on
     * it.
     * <p>
     * A search is a pass over every test case in the project, and that does not
     * belong on the thread painting the dialog - a large project made the field
     * stutter as the tester typed. Each request takes the next generation, so a
     * slow answer that lands after a newer keystroke is dropped rather than
     * painted over the newer one. A fixed set answers instantly and takes the
     * same path, harmlessly.
     */
    private void requestRows() {
        if (!panel.isShowing()) return;

        final int generation = queryGeneration.incrementAndGet();
        final @NotNull String query = textField.getText().trim();

        // This dialog's own modality, read here while it is on screen. A runnable
        // posted without it is queued behind the open dialog and runs only once
        // that dialog closes - which, for the list the dialog exists to show,
        // means never.
        final @NotNull ModalityState modality = ModalityState.stateForComponent(panel);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull List<SelectionList<T>> found = rows.forQuery(query);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != queryGeneration.get() || !panel.isShowing()) return;
                show(found);
            }, modality);
        });
    }

    /**
     * Puts these rows on screen, with the first one selected - which is what
     * Enter takes, so it has to be a row the tester can see.
     */
    private void show(final @NotNull List<SelectionList<T>> found) {
        // A fixed set of choices answers the same rows every time the tester
        // types, and replacing them only to drop back to the first would throw
        // away the kind they picked above the name they are entering - a run made
        // "Test Run" however they set it. Rows that did not change leave the
        // selection alone; a search, whose rows do change, still opens on the top
        // row, which is what Enter takes.
        if (found.equals(rowModel.getItems())) return;

        rowModel.replaceAll(found);

        if (found.isEmpty()) return;

        list.setSelectedIndex(0);
        list.ensureIndexIsVisible(0);
    }

    /**
     * What the tester picked, and empty when a search found nothing.
     * <p>
     * Separate from {@link #getSelectedValue()} because they answer different
     * questions: a dialog offering a fixed set of choices always has one, and a
     * dialog offering what a query matched may have none, which is an ordinary
     * thing for a search rather than a failure.
     */
    public @NotNull Optional<T> selection() {
        return ListValue.selected(list).map(SelectionList::value);
    }

    public @NotNull String getText() {
        return textField.getText();
    }

    public void showEmptyWarning() {
        input.showEmptyWarning();
    }

    /**
     * What the tester picked, for a dialog whose rows are fixed and therefore
     * never empty. A search asks {@link #selection()} instead.
     */
    public @NotNull T getSelectedValue() {
        // A single-selection list can still be emptied (e.g. Ctrl+click on the
        // selected row); the first row is the declared default.
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
        // setExtensions does not refresh the field on its own; without this
        // the icon goes stale when the selection moves by keyboard.
        ListValue.selected(list).ifPresent(selected -> {
            DialogStyle.setLeadingIcon(textField, selected.icon());
            textField.revalidate();
            textField.repaint();
        });
    }

    /**
     * Cut, copy and paste in the field.
     * <p>
     * A field inside a popup does not always inherit them: the popup and the
     * dialog both bind keys on the way to it, and what reaches the text
     * component is whatever they left. Bound by name to the actions the text
     * component already has, so this asks for the standard behavior rather than
     * writing a second one - and a tester can paste a test case id or a ticket
     * number into the search instead of typing it out (#29).
     */
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
                // Platform hint standard: theme-aware gray, italic.
                append("   " + value.hint(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
            }
            setBorder(JBUI.Borders.empty(8, 12));
        }
    }
}
