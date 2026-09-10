package org.testin.testrun;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunStatus;
import org.testin.ui.dialogs.ShortcutMenuPopup;

import java.util.Arrays;
import java.util.function.Consumer;

@AllArgsConstructor
public class TestRunStatusMenuDialog {

    private final @NotNull Project p;

    /**
     * Where the run is now, which decides what it can be moved to.
     */
    private final @NotNull TestRunStatus current;

    private final @NotNull Consumer<TestRunStatus> onStatusSelected;

    /**
     * UC-TREE-PANEL-020, Rule-TREE-PANEL-068, Rule-TREE-PANEL-092.
     * <p>
     * The statuses this run can be moved to, and no others.
     * <p>
     * It offered all five. Two of them are the run's own record of itself -
     * Created when it is made, In Progress when execution starts - so the menu
     * invited a tester to declare something that had either happened or not; and
     * a run could be sent from Assigned back to Created, which un-says it (#186).
     * <p>
     * Asked of the status rather than tested for here, so the lifecycle is
     * written once on the thing that has one.
     */
    public void show() {
        final TestRunStatus @NotNull [] offered = Arrays.stream(TestRunStatus.values())
                .filter(status -> status.canBeSetFrom(current))
                .toArray(TestRunStatus[]::new);

        new ShortcutMenuPopup<>(p, "Set Test Run Status", offered, onStatusSelected).show();
    }
}
