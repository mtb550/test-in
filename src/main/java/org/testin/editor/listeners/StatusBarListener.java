package org.testin.editor.listeners;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;

import javax.swing.*;
import java.util.Optional;

public class StatusBarListener {

    public static void attach(final @NotNull TestinEditor editor) {
        editor.getStatusBar().getFirstButton().addActionListener(e -> {
            editor.setCurrentPage(1);
            editor.refreshView();
        });

        editor.getStatusBar().getPrevButton().addActionListener(e -> {
            if (editor.getCurrentPage() > 1) {
                editor.setCurrentPage(editor.getCurrentPage() - 1);
                editor.refreshView();
            }
        });

        editor.getStatusBar().getNextButton().addActionListener(e -> {
            if (editor.getCurrentPage() < editor.getTotalPageCount()) {
                editor.setCurrentPage(editor.getCurrentPage() + 1);
                editor.refreshView();
            }
        });

        editor.getStatusBar().getLastButton().addActionListener(e -> {
            editor.setCurrentPage(editor.getTotalPageCount());
            editor.refreshView();
        });

        editor.getStatusBar().getPageSizeField().addActionListener(e -> {
            try {
                final int newSize = Integer.parseInt(editor.getStatusBar().getPageSizeField().getText().trim());
                if (newSize > 0) {
                    editor.setPageSize(newSize);
                    editor.setCurrentPage(1);
                    editor.refreshView();
                }
            } catch (final NumberFormatException ex) {
                editor.getStatusBar().getPageSizeField().setText(String.valueOf(editor.getPageSize()));
            }

            Optional.ofNullable(editor.getPreferredFocusedComponent()).ifPresent(JComponent::requestFocusInWindow);
        });
    }
}