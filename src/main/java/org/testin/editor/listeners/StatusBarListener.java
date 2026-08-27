package org.testin.editor.listeners;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;


public class StatusBarListener {

    public static void attach(final @NotNull TestinEditor editor) {
        // All four are the editor's one page step with a different delta -
        // first and last compute theirs from where the tester is now. The bounds
        // check went with them: the step refuses to leave the range itself.
        editor.getStatusBar().getFirstButton().addActionListener(e -> editor.stepPage(1 - editor.getCurrentPage()));
        editor.getStatusBar().getPrevButton().addActionListener(e -> editor.stepPage(-1));
        editor.getStatusBar().getNextButton().addActionListener(e -> editor.stepPage(1));
        editor.getStatusBar().getLastButton().addActionListener(e -> editor.stepPage(editor.getTotalPageCount() - editor.getCurrentPage()));

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

            editor.getPreferredFocusedComponent().requestFocusInWindow();
        });
    }
}