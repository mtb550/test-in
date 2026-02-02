package testGit.ui;

import com.intellij.openapi.ui.MessageDialogBuilder;
import testGit.pojo.Config;
import testGit.pojo.TestCase;

import java.util.List;
import java.util.stream.Collectors;

public class DeleteTestCaseDialog {

    public static boolean confirmDeleteAction(List<TestCase> selected) {
        if (selected == null || selected.isEmpty()) return false;

        String title = selected.size() == 1 ? "Delete Test Case" : "Delete Test Cases";
        String message;

        if (selected.size() == 1) {
            message = "Are you sure you want to delete '" + selected.get(0).getTitle() + "'?";
        } else {
            // Limit the display to first 5 items so the dialog doesn't overflow
            String displayedTitles = selected.stream()
                    .limit(5)
                    .map(tc -> "• " + tc.getTitle())
                    .collect(Collectors.joining("\n"));

            if (selected.size() > 5) {
                displayedTitles += "\n...and " + (selected.size() - 5) + " more.";
            }

            message = "Are you sure you want to delete these " + selected.size() + " test cases?\n\n" + displayedTitles;
        }

        return MessageDialogBuilder.yesNo(title, message)
                .yesText("Delete")
                .noText("Cancel")
                .asWarning() // Critical for theme-consistent warning icons
                .ask(Config.getProject());
    }
}