package org.testin.ui.createNodes;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBUI;
import org.testin.pojo.DirectoryType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public record DialogListSelectionListenerImpl(
        ExtendableTextField textField,
        JBList<DirectoryType> list
) implements ListSelectionListener {

    public DialogListSelectionListenerImpl(final ExtendableTextField textField, final JBList<DirectoryType> list) {
        this.textField = textField;
        this.list = list;

        updateIcon();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateIcon();
    }

    private void updateIcon() {
        final DirectoryType selected = list.getSelectedValue();

        if (selected == null) return;

        textField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return selected.getIcon();
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        });

        textField.repaint();
    }
}