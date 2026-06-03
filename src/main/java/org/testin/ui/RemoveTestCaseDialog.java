package org.testin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveTestCaseDialog {

    public static boolean confirmDeleteAction(final @NotNull Project project, final List<TestCaseDto> selected) {
        if (selected == null || selected.isEmpty()) return false;

        final String title = selected.size() == 1 ? "Delete Test Case" : "Delete Test Cases";
        String message;

        if (selected.size() == 1) {
            message = "Are you sure you want to delete\n'" + selected.getFirst().getDescription() + "'?";
        } else {
            String displayedDescription = selected.stream()
                    .map(tc -> ". " + tc.getDescription())
                    .collect(Collectors.joining("\n"));

            message = "Are you sure you want to delete these " + selected.size() + " test cases?\n\n" + displayedDescription;
        }

        return MessageDialogBuilder.yesNo(title, message)
                .yesText("Delete")
                .noText("Cancel")
                .asWarning()
                .ask(project);
    }
}