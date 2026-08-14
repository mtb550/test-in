package org.testin.testRun;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestRunStatus;
import org.testin.ui.dialogs.ShortcutMenuPopup;

import java.util.function.Consumer;

@AllArgsConstructor
public class TestRunStatusMenuDialog {

    private final @NotNull Project p;

    private final @NotNull Consumer<TestRunStatus> onStatusSelected;

    public void show() {
        new ShortcutMenuPopup<>(p, "Set Test Run Status", TestRunStatus.values(),
                TestRunStatus::getIcon,
                TestRunStatus::getLabel,
                TestRunStatus::getShortcutText,
                TestRunStatus::bindShortcut,
                onStatusSelected).show();
    }
}
