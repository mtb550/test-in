package org.testin.mappers.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.markers.TestRunMarker;


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
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TR.getMarker();
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
}
