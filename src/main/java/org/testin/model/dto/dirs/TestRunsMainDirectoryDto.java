package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.markers.TestRunsMainDirectoryMarker;


@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TestRunsMainDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestRunsMainDirectoryMarker marker = new TestRunsMainDirectoryMarker();




    @Override
    public boolean isRenamable() {
        return false;
    }

    @Override
    public boolean isTransferable() {
        return false;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @Override
    public @NotNull String getMarkerFileName() {
        return DirectoryType.TRD.getMarker();
    }

    @Override
    public boolean acceptsTransferred(final @NotNull DirectoryDto source) {
        // Test-set nodes never land in the run family.
        return super.acceptsTransferred(source) && source.isAllowedInTestRunFamily();
    }

    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TRD;
    }

    @Override
    public boolean canCreateChildren() {
        return true;
    }
}
