package org.testin.indexer;

import org.testin.model.PackageStatus;
import org.testin.model.markers.Marker;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.dirs.*;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/**
 * A retired node - a deprecated test set, an archived package - is one thing to
 * the plugin: the DTO answers {@code isRetired()} from its own marker's status,
 * and the children index sorts the retired ones after the live ones (#68).
 */
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

    /**
     * The number a tester typed decides the folder, and the ones they left alone
     * follow by the date they were made - which is how the folder read before
     * anybody typed anything.
     */
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

    /**
     * Two nodes with the same number is not a problem to solve - the date
     * decides between them, so a tester can put a set third without renumbering
     * the set that was already third.
     */
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

    /**
     * A number never lifts a retired node above a live one: what is finished
     * stays out of the way of what is not.
     */
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

    /**
     * Everything a tester puts in a folder can be numbered. What cannot is what
     * has no arrangement to have: a project's two containers are exactly two and
     * always the same way round, and the project is what the tree is rooted at.
     */
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

    /**
     * A node nobody numbered carries the largest number there is, so it sorts
     * after every number a tester did give without anything having to test for
     * it. A marker written before this existed reads the same way.
     */
    @Test
    public void aNodeNobodyNumberedSortsAfterEveryNumber() {
        assertEquals(testSet("a", TestSetStatus.ACTIVE).getOrder(), Marker.NOT_ORDERED);
    }

    /**
     * Days apart, so the comparison is about the date and not about the second
     * the test happened to run in.
     */
    private static <T extends DirectoryDto> T createdAt(final T node, final int daysAgo) {
        node.getMarker().setCreatedAt(ZonedDateTime.now().minusDays(daysAgo));
        return node;
    }
}
