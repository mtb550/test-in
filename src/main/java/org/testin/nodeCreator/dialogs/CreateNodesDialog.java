package org.testin.nodeCreator.dialogs;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.CreateNodeMenu;
import org.testin.enums.DirectoryType;
import org.testin.ui.dialogs.AbstractInputPopupDialog;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Create-node popup built on the shared dynamic input dialog.
 */
public final class CreateNodesDialog extends AbstractInputPopupDialog {

    private final @NotNull JBList<DirectoryType> list;
    private final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onSubmit;

    public CreateNodesDialog(final @NotNull Project project,
                             final @NotNull CreateNodeMenu menu,
                             final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onSubmit) {
        super(project, menu.getTitle(), AllIcons.General.Add, menu.getPlaceholder(), "");
        this.onSubmit = onSubmit;

        final List<DirectoryType> options = menu.getAvailableOptions();
        list = new JBList<>(options.toArray(new DirectoryType[0]));
        list.setBorder(JBUI.Borders.empty(6));
        list.setFont(JBUI.Fonts.label().deriveFont(JBUI.Fonts.label().getSize2D() + 2f));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(options.size());
        list.setCellRenderer(new DialogListCellRendererImpl());

        setLeadingIcon(menu.getTargetParentType().getIcon());
        list.addListSelectionListener(new DialogListSelectionListenerImpl(textField, list));
        list.addMouseListener(new DialogMouseAdapterImpl(list, this::submitInput));
        textField.addKeyListener(new DialogKeyListenerImpl(list));

        final JBPanel<?> listWrapper = new JBPanel<>(new BorderLayout());
        listWrapper.add(list, BorderLayout.CENTER);

        final JBScrollPane scrollPane = new JBScrollPane(listWrapper);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        addContent(scrollPane, BorderLayout.CENTER);

        initializeInputPopup();
    }

    @Override
    protected void onSubmit(final @NotNull String value) {
        onSubmit.accept(value, list.getSelectedValue());
    }
}
