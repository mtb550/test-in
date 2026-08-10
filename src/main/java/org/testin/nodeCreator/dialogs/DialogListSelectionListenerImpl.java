package org.testin.nodeCreator.dialogs;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.testin.enums.DirectoryType;
import org.testin.ui.dialogs.DialogStyle;

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

        DialogStyle.setLeadingIcon(textField, selected.getIcon());
    }
}
