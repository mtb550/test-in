package org.testin.testRun;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestRunStatus;
import org.testin.ui.dialogs.ShortcutMenuPopup;

import java.util.function.Consumer;

public class TestRunStatusMenuDialog {

    private final @NotNull Project p;

    private final Consumer<TestRunStatus> onStatusSelected;

    public TestRunStatusMenuDialog(final @NotNull Project p, final Consumer<TestRunStatus> onStatusSelected) {
        this.p = p;
        this.onStatusSelected = onStatusSelected;
    }

    public void show() {
        new ShortcutMenuPopup<>(p, "Set Test Run Status", TestRunStatus.values(),
                TestRunStatus::getIcon,
                TestRunStatus::getLabel,
                TestRunStatus::getShortcutText,
                TestRunStatus::bindShortcut,
                onStatusSelected).show();
    }
}
