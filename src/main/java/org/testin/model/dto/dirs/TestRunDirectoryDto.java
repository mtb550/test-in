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
        return true;
    }
}
