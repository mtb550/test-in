package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.TestSetStatus;
import org.testin.model.markers.TestSetMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestSetDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestSetMarker marker = new TestSetMarker();


    @Override
    public boolean isTestCaseContainer() {
        return true;
    }

    @Override
    public boolean isOpenableInEditor() {
        return true;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TS.getMarker();
    }

    @Override
    public boolean isTransferTarget() {
        // A test set holds test cases only - no directory node ever lands
        // inside it (not a package, not another test set, not run nodes).
        return false;
    }

    @Override
    public boolean isAllowedInTestRunFamily() {
        return false;
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TS;
    }

    @Override
    public boolean isRetired() {
        return marker.getStatus() == TestSetStatus.DEPRECATED;
    }

    /**
     * A test set is arranged by the tester: the order of a suite is a plan for
     * working through it, not an accident of naming.
     */
    @Override
    public boolean isOrderable() {
        return true;
    }
}
