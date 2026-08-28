package org.testin.editor.listeners;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditor;


public class StatusBarListener {

    public static void attach(final @NotNull TestinEditor editor) {
        // What the editor is actually paging by. The field used to be built with
        // "50" written into it and was only ever corrected after a tester typed
        // something unparseable, so it stated a number rather than reporting one -
        // and would have gone on stating fifty whatever the editor was using.
        editor.getStatusBar().getPageSizeField().setText(String.valueOf(editor.getPageSize()));

        // All four are the editor's one page step with a different delta -
        // first and last compute theirs from where the tester is now. The bounds
        // check went with them: the step refuses to leave the range itself.
        editor.getStatusBar().getFirstButton().addActionListener(e -> editor.stepPage(1 - editor.getCurrentPage()));
        editor.getStatusBar().getPrevButton().addActionListener(e -> editor.stepPage(-1));
        editor.getStatusBar().getNextButton().addActionListener(e -> editor.stepPage(1));
        editor.getStatusBar().getLastButton().addActionListener(e -> editor.stepPage(editor.getTotalPageCount() - editor.getCurrentPage()));

        editor.getStatusBar().getPageSizeField().addActionListener(e -> {
            // Every value is accepted and corrected in place, so the field can
            // never sit there showing a number the editor is not paging by. It
            // used to leave whatever was typed alone unless it failed to parse,
            // so "0" and "-3" stayed on screen while the editor kept the old
            // size - the field disagreeing with the list underneath it.
            final int size = TestinEditor.pageSizeOf(editor.getStatusBar().getPageSizeField().getText());
            editor.getStatusBar().getPageSizeField().setText(String.valueOf(size));

            // Silent, and deliberately so: this only changes how much of the list
            // is drawn at once, and the corrected number in the field is the whole
            // of what the tester needs to be told.
            if (size != editor.getPageSize()) {
                editor.setPageSize(size);
                editor.setCurrentPage(1);
                editor.refreshView();
            }

            editor.getPreferredFocusedComponent().requestFocusInWindow();
        });
    }
}