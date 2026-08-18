package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.PackageStatus;
import org.testin.model.markers.TestRunPackageMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunPackageDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestRunPackageMarker marker = new TestRunPackageMarker();


    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TRP.getMarker();
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
        // Test-set nodes never land in the run family.
        return super.acceptsTransferred(source) && source.isAllowedInTestRunFamily();
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TRP;
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
