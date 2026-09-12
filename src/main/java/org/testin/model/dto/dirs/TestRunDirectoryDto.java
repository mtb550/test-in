/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    /**
     * UC-TREE-PANEL-012, Rule-TREE-PANEL-094.
     * <p>
     * <b>A run can always be removed, whatever it has been signed off as.</b>
     * <p>
     * It used to answer {@code isStillOpen()} with the three above, and that was
     * one restriction too many. The other three keep a report honest: a run that
     * is renamed, renumbered or dragged somewhere else is still there, still
     * named in a report, and now described wrongly - the reader has no way to
     * know. A removed run is not misdescribed, it is gone, and a reader who
     * cannot find it knows exactly that.
     * <p>
     * It was also the rule nobody could see. Rule-TREE-PANEL-009 says a
     * signed-off run's cases, verdicts and configuration cannot change, and says
     * nothing about removing it - so the entry grayed for a reason written in
     * this class and nowhere a tester could read. Half of #184, reversed
     * deliberately on 10 September 2026.
     * <p>
     * Stated on the method it is about. It sat on {@link #isTransferable} for an
     * evening, telling a reader that dragging a signed-off run was allowed while
     * the line underneath refused it.
     */
    @Override
    public boolean isRemovable() {
        return true;
    }

    /**
     * UC-TREE-PANEL-013, Rule-TREE-PANEL-009.
     * <p>
     * A signed-off run stays where it is. Dragged somewhere else it is still
     * named in a report, at a place it no longer sits, and the reader has no way
     * to know - the same reason it refuses a rename and a new number, and the
     * reason removing it is a different question.
     */
    @Override
    public boolean isTransferable() {
        return isStillOpen();
    }
}
