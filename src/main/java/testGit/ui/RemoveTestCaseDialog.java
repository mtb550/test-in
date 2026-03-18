package testGit.ui;

import com.intellij.openapi.ui.MessageDialogBuilder;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveTestCaseDialog {

    public static boolean confirmDeleteAction(List<TestCaseDto> selected) {
        if (selected == null || selected.isEmpty()) return false;

        String title = selected.size() == 1 ? "Delete Test Case" : "Delete Test Cases";
        String message;

        if (selected.size() == 1) {
            message = "Are you sure you want to delete\n'" + selected.get(0).getTitle() + "'?";
        } else {
            String displayedTitles = selected.stream()
                    .map(tc -> "• " + tc.getTitle())
                    .collect(Collectors.joining("\n"));

            message = "Are you sure you want to delete these " + selected.size() + " test cases?\n\n" + displayedTitles;
        }

        return MessageDialogBuilder.yesNo(title, message)
                .yesText("Delete")
                .noText("Cancel")
                .asWarning()
                .ask(Config.getProject());
    }
}