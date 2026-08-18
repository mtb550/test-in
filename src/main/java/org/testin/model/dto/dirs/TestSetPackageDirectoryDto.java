package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.PackageStatus;
import org.testin.model.markers.TestSetPackageMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestSetPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestSetPackageMarker marker = new TestSetPackageMarker();


    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TSP.getMarker();
    }

    @Override
    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        // Run nodes never land in the test-set family.
        return super.acceptsTransferred(source) && source.isAllowedInTestSetFamily();
    }

    @Override
    public boolean isAllowedInTestRunFamily() {
        return false;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TSP;
    }

    @Override
    public boolean canCreateChildren() {
        return true;
    }

    @Override
    public boolean isRetired() {
        return marker.getStatus() == PackageStatus.ARCHIVED;
    }
}
