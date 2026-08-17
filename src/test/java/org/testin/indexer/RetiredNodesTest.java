package org.testin.indexer;

import org.testin.model.PackageStatus;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.dirs.*;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

        final List<DirectoryDto> sorted = new DirectoryChildrenIndex().get(PARENT,
                () -> Stream.concat(Stream.of(parent), children.stream()).toList());

        assertEquals(sorted.stream().map(DirectoryDto::getName).toList(), List.of("beta", "zeta", "alpha", "old"));
    }
}
