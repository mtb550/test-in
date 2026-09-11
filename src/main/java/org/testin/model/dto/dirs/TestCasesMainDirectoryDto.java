package org.testin.model.dto.dirs;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.markers.TestCasesMainDirectoryMarker;


@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@SuperBuilder
public class TestCasesMainDirectoryDto extends DirectoryDto {
    @NonNull
    @Builder.Default
    private TestCasesMainDirectoryMarker marker = new TestCasesMainDirectoryMarker();


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
    public boolean isTestCaseContainer() {
        return true;
    }



    @Override
    public @NotNull DirectoryType getType() {
        return DirectoryType.TCD;
    }

    @Override
    public boolean canCreateChildren() {
        return true;
    }
}
