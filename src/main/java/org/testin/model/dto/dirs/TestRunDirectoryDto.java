package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.markers.TestRunMarker;

import java.nio.file.Path;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunDirectoryDto extends DirectoryDto {

    @NotNull
    @Builder.Default
    private TestRunMarker marker = new TestRunMarker();


    @Override
    public boolean isOpenableInEditor() {
        return true;
    }

    /**
     * Whether the run still accepts changes - a verdict, which cases it covers,
     * its name, its configuration.
     * <p>
     * A completed or closed run has been signed off and reported on, and what a
     * report says must not move underneath it afterwards (#84). Three actions
     * asked this by spelling out {@code !getMarker().getStatus().isTerminal()};
     * asking the node instead is what keeps them agreeing as statuses are added.
     */
    public boolean isStillOpen() {
        return !marker.getStatus().isTerminal();
    }


    @Override
    public boolean isAllowedInTestSetFamily() {
        return false;
    }

    @Override
    public boolean isAllowedInsideTestRun() {
        return false;
    }

    @Override
    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        // Test-set nodes never land in the run family, and a test run
        // never lands inside another test run.
        return super.acceptsTransferred(source)
                && source.isAllowedInTestRunFamily()
                && source.isAllowedInsideTestRun();
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TR;
    }

    /**
     * Where a run's results live: {@code <run folder>/run.json}, whatever the
     * folder is called.
     * <p>
     * The name used to be the folder's own - {@code Cycle-1/Cycle-1.json} - which
     * made a run the only node in the tree whose contents were named after it, and
     * so the only node a rename could empty. It did: the write derived that name
     * and the scan's read derived it again, and neither told the rename, so
     * renaming a cycle moved the folder, left the results behind under the old
     * name, and the next index found nothing where a whole cycle had been (#177).
     * <p>
     * Fixing the rename would have kept the trap for whatever moved a node next.
     * A fixed name has nothing to keep in step, so there is no longer a rule to
     * forget - the run now behaves like every other node, whose contents are named
     * independently of the folder ({@code <id>.json} for a test case).
     */
    public static @NotNull Path resultsFile(final @NotNull Path runPath) {
        return runPath.resolve("run.json");
    }

    /**
     * A test run is arranged by the tester when they say so. Unnumbered it reads by
     * the date it was created, which is the order runs have always had - the
     * number is for the cycle somebody wants at the top.
     */
    @Override
    public boolean isOrderable() {
        return isStillOpen();
    }

    /**
     * UC-TREE-PANEL-025, Rule-TREE-PANEL-009.
     * <p>
     * A signed-off run does not change, and its name, its place among its
     * siblings and its existence are part of what it is.
     * <p>
     * The verdict half of that rule was already kept: a Completed or Closed run
     * refuses verdicts, edits and execution. The tree half was not, so the same
     * run could be renamed, dragged somewhere else, given a different number and
     * removed - and a report naming it by name and place moved underneath the
     * person reading it (#184, and #84 for the other half).
     * <p>
     * Asked of the node rather than checked in the four actions, so a fifth
     * gesture cannot forget: rename, order, remove and drag each already ask the
     * node whether it allows them.
     */
    @Override
    public boolean isRenamable() {
        return isStillOpen();
    }

    @Override
    public boolean isRemovable() {
        return isStillOpen();
    }

    @Override
    public boolean isTransferable() {
        return isStillOpen();
    }
}
