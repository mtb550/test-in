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

package org.testin.indexer;

import org.testin.model.PackageStatus;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.model.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.model.markers.Marker;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RetiredNodesTest {

    private static final Path PARENT = Path.of("root", "Test Cases");

    private static TestSetDirectoryDto testSet(final String name, final TestSetStatus status) {
        final TestSetDirectoryDto dto = new TestSetDirectoryDto();
        dto.setName(name);
        dto.setPath(PARENT.resolve(name));
        dto.getMarker().setStatus(status);
        return dto;
    }

    private static TestSetPackageDirectoryDto testSetPackage(final String name, final PackageStatus status) {
        final TestSetPackageDirectoryDto dto = new TestSetPackageDirectoryDto();
        dto.setName(name);
        dto.setPath(PARENT.resolve(name));
        dto.getMarker().setStatus(status);
        return dto;
    }

    private static <T extends DirectoryDto> T createdAt(final T node, final int daysAgo) {
        node.getMarker().setCreatedAt(ZonedDateTime.now().minusDays(daysAgo));
        return node;
    }

    @Test
    public void theStatusOnTheMarkerDecidesWhetherTheNodeIsRetired() {
        assertFalse(testSet("a", TestSetStatus.ACTIVE).isRetired());
        assertTrue(testSet("a", TestSetStatus.DEPRECATED).isRetired());

        assertFalse(testSetPackage("p", PackageStatus.ACTIVE).isRetired());
        assertTrue(testSetPackage("p", PackageStatus.ARCHIVED).isRetired());

        final TestRunPackageDirectoryDto runPackage = new TestRunPackageDirectoryDto();
        runPackage.getMarker().setStatus(PackageStatus.ARCHIVED);
        assertTrue(runPackage.isRetired());
    }

    @Test
    public void nodesWithoutAStatusAreNeverRetired() {
        for (final DirectoryDto fixed : List.of(new TestProjectDirectoryDto(), new TestCasesMainDirectoryDto(),
                new TestRunsMainDirectoryDto(), new TestRunDirectoryDto())) {
            assertFalse(fixed.isRetired(), fixed.getClass().getSimpleName());
        }
    }

    @Test
    public void retiredChildrenSortAfterTheLiveOnesAndByNameWithinEach() {
        final TestCasesMainDirectoryDto parent = new TestCasesMainDirectoryDto();
        parent.setPath(PARENT);

        final List<DirectoryDto> children = List.of(
                testSetPackage("old", PackageStatus.ARCHIVED),
                testSet("zeta", TestSetStatus.ACTIVE),
                testSet("alpha", TestSetStatus.DEPRECATED),
                testSetPackage("beta", PackageStatus.ACTIVE));
        children.forEach(child -> child.setParent(parent));

        final List<DirectoryDto> ordered = new DirectoryChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(DirectoryDto::getName).toList(), List.of("beta", "zeta", "alpha", "old"));
    }

    @Test
    public void numberedChildrenComeFirstAndTheRestFollowByDate() {
        final TestCasesMainDirectoryDto parent = new TestCasesMainDirectoryDto();
        parent.setPath(PARENT);

        final TestSetDirectoryDto third = createdAt(testSet("aaa-oldest", TestSetStatus.ACTIVE), 3);
        final TestSetDirectoryDto fourth = createdAt(testSet("bbb-newest", TestSetStatus.ACTIVE), 1);
        final TestSetDirectoryDto first = testSet("zzz-numbered-one", TestSetStatus.ACTIVE);
        final TestSetDirectoryDto second = testSet("yyy-numbered-two", TestSetStatus.ACTIVE);

        first.getMarker().setOrder(1);
        second.getMarker().setOrder(2);

        final List<DirectoryDto> children = List.of(third, fourth, first, second);
        children.forEach(child -> child.setParent(parent));

        final List<DirectoryDto> ordered = new DirectoryChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(DirectoryDto::getName).toList(),
                List.of("zzz-numbered-one", "yyy-numbered-two", "aaa-oldest", "bbb-newest"),
                "numbers first, in order; then the unnumbered ones oldest first, whatever they are called");
    }

    @Test
    public void theSameNumberTwiceIsSettledByTheDate() {
        final TestCasesMainDirectoryDto parent = new TestCasesMainDirectoryDto();
        parent.setPath(PARENT);

        final TestSetDirectoryDto older = createdAt(testSet("zzz-older", TestSetStatus.ACTIVE), 5);
        final TestSetDirectoryDto newer = createdAt(testSet("aaa-newer", TestSetStatus.ACTIVE), 1);

        older.getMarker().setOrder(2);
        newer.getMarker().setOrder(2);

        final List<DirectoryDto> children = List.of(newer, older);
        children.forEach(child -> child.setParent(parent));

        final List<DirectoryDto> ordered = new DirectoryChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(DirectoryDto::getName).toList(), List.of("zzz-older", "aaa-newer"));
    }

    @Test
    public void aNumberDoesNotBringARetiredNodeBack() {
        final TestCasesMainDirectoryDto parent = new TestCasesMainDirectoryDto();
        parent.setPath(PARENT);

        final TestSetDirectoryDto retired = testSet("deprecated", TestSetStatus.DEPRECATED);
        retired.getMarker().setOrder(1);

        final TestSetDirectoryDto live = testSet("active", TestSetStatus.ACTIVE);

        final List<DirectoryDto> children = List.of(retired, live);
        children.forEach(child -> child.setParent(parent));

        final List<DirectoryDto> ordered = new DirectoryChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(ordered.stream().map(DirectoryDto::getName).toList(), List.of("active", "deprecated"));
    }

    @Test
    public void everythingATesterFilesCanBeOrdered() {
        for (final DirectoryDto node : List.of(testSet("a", TestSetStatus.ACTIVE),
                testSetPackage("p", PackageStatus.ACTIVE), new TestRunDirectoryDto(),
                new TestRunPackageDirectoryDto())) {
            assertTrue(node.isOrderable(), node.getClass().getSimpleName());
        }

        for (final DirectoryDto fixed : List.of(new TestProjectDirectoryDto(), new TestCasesMainDirectoryDto(),
                new TestRunsMainDirectoryDto())) {
            assertFalse(fixed.isOrderable(), fixed.getClass().getSimpleName());
        }
    }

    @Test
    public void aNodeNobodyNumberedSortsAfterEveryNumber() {
        assertEquals(testSet("a", TestSetStatus.ACTIVE).getOrder(), Marker.NOT_ORDERED);
    }
}
