package testGit.editorPanel.listeners;

import testGit.editorPanel.BaseEditorUI;

public class StatusBarListener {

    public static void attach(final BaseEditorUI ui) {
        ui.getStatusBar().getFirstButton().addActionListener(e -> {
            ui.setCurrentPage(1);
            ui.refreshView();
        });

        ui.getStatusBar().getPrevButton().addActionListener(e -> {
            if (ui.getCurrentPage() > 1) {
                ui.setCurrentPage(ui.getCurrentPage() - 1);
                ui.refreshView();
            }
        });

        ui.getStatusBar().getNextButton().addActionListener(e -> {
            if (ui.getCurrentPage() < ui.getTotalPageCount()) {
                ui.setCurrentPage(ui.getCurrentPage() + 1);
                ui.refreshView();
            }
        });

        ui.getStatusBar().getLastButton().addActionListener(e -> {
            ui.setCurrentPage(ui.getTotalPageCount());
            ui.refreshView();
        });

        ui.getStatusBar().getPageSizeField().addActionListener(e -> {
            try {
                int newSize = Integer.parseInt(ui.getStatusBar().getPageSizeField().getText().trim());
                if (newSize > 0) {
                    ui.setPageSize(newSize);
                    ui.setCurrentPage(1);
                    ui.refreshView();
                }
            } catch (NumberFormatException ex) {
                ui.getStatusBar().getPageSizeField().setText(String.valueOf(ui.getPageSize()));
            }

            if (ui.getPreferredFocusedComponent() != null) {
                ui.getPreferredFocusedComponent().requestFocusInWindow();
            }
        });
    }
}