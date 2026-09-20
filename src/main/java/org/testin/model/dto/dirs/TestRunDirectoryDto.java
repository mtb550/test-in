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
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    private static final @NotNull String SCREENSHOT_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";

    private static final int SCREENSHOT_NAME_LENGTH = 5;

    private static final @NotNull Pattern SCREENSHOT_NAME = Pattern.compile("[0-9a-z]{" + SCREENSHOT_NAME_LENGTH + "}\\.png");

    /**
     * UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-219.
     * <p>
     * The name a newly pasted screenshot is kept under in its run's folder: five
     * random lowercase letters and digits, then {@code .png}, and none of the
     * names in {@code taken} (#313).
     * <p>
     * Short, because a tester reads it: it is the screenshot's link in the view
     * panel. Five characters give some sixty million names, so the names the run
     * already holds are the only ones worth checking. Nothing in it names the run
     * or its folder, so a rename or a move leaves it valid - which is the lesson
     * of the run file that was named after its run, and had to be found, renamed
     * and moved with it until #305 gave every record a name of its own.
     */
    public static @NotNull String newScreenshotName(final @NotNull Set<String> taken) {
        return Stream.generate(TestRunDirectoryDto::randomScreenshotName)
                .filter(name -> !taken.contains(name))
                .findFirst()
                .orElseThrow();
    }

    private static @NotNull String randomScreenshotName() {
        return ThreadLocalRandom.current().ints(SCREENSHOT_NAME_LENGTH, 0, SCREENSHOT_CHARACTERS.length())
                .mapToObj(index -> String.valueOf(SCREENSHOT_CHARACTERS.charAt(index)))
                .collect(Collectors.joining()) + ".png";
    }

    /**
     * Whether a file name is one {@link #newScreenshotName} gives - the name rule,
     * where the names are made. <b>Whether a file is a screenshot</b> is
     * {@code FileKind.of(file, folder)}, which also knows it counts only inside a
     * run: any PNG named this way in a run's folder is taken for a screenshot, so
     * one a tester put there by hand under such a name goes when no result names
     * it, while a five-character picture beside a test set is a file like any
     * other (#305, S29).
     */
    public static boolean isScreenshotName(final @NotNull String fileName) {
        return SCREENSHOT_NAME.matcher(fileName).matches();
    }

    /**
     * Where a screenshot of this run lives: beside the result that names it.
     */
    public static @NotNull Path screenshotFile(final @NotNull Path runPath, final @NotNull String name) {
        return runPath.resolve(name);
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
