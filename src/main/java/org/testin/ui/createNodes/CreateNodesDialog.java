package org.testin.ui.createNodes;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.testin.pojo.Config;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.DirectoryType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

public class CreateNodesDialog {
    final Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 6f);
    final Border fieldBorder = JBUI.Borders.empty(10);
    final Font optionsFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);
    final Border optionsBorder = JBUI.Borders.empty(6);
    final Dimension minSize = new Dimension(JBUI.scale(350), 0);
    private final ExtendableTextField textField;
    private final JBList<DirectoryType> list;
    private final JBPopup popup;

    public CreateNodesDialog(final CreateNodeMenu menu, final JComponent settingButton, final BiConsumer<String, DirectoryType> onSelected) {
        textField = new ExtendableTextField();

        textField.setFont(fieldFont);
        textField.setBorder(fieldBorder);

        textField.getEmptyText().setText(menu.getPlaceholder());
        TextComponentEmptyText.setupPlaceholderVisibility(textField);

        List<DirectoryType> optionsList = menu.getAvailableOptions();
        list = new JBList<>(optionsList.toArray(new DirectoryType[0]));

        list.setBorder(optionsBorder);
        list.setFont(optionsFont);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(optionsList.size());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty());

        mainPanel.add(textField, BorderLayout.NORTH);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.add(list, BorderLayout.CENTER);

        JBScrollPane scrollPane = new JBScrollPane(listWrapper);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(mainPanel, textField)
                .setTitle(menu.getTitle())
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setCancelOnClickOutside(true)
                .setMovable(false)
                .setResizable(false)
                // todo, dispose to be implemented
                /*.addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        dispose();
                    }
                })*/
                .setMinSize(minSize);

        if (settingButton != null) {
            builder.setSettingButtons(settingButton);
        }

        popup = builder.createPopup();

        final Runnable submitAction = () -> executeSubmitAction(onSelected);

        textField.addKeyListener(new DialogKeyListenerImpl(list, popup, submitAction));
        list.addMouseListener(new DialogMouseAdapterImpl(list, submitAction));
        list.setCellRenderer(new DialogListCellRendererImpl());
        list.addListSelectionListener(new DialogListSelectionListenerImpl(textField, list));
    }

    public void show() {
        popup.showCenteredInCurrentWindow(Config.getProject());
        SwingUtilities.invokeLater(() -> {
            textField.revalidate();
            textField.repaint();
        });
    }

    private void executeSubmitAction(final BiConsumer<String, DirectoryType> onSelected) {
        final String text = textField.getText().trim();

        if (!text.isEmpty()) {
            onSelected.accept(text, list.getSelectedValue());
            popup.closeOk(null);

        } else textField.requestFocus();
    }
}