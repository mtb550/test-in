package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.PackageStatus;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestSetPackageMarker extends AbstractMarker implements PackageMarker {
    /**
     * Archived packages keep everything inside them and sort after the active
     * ones, left collapsed (#68).
     */
    @NonNull
    private PackageStatus status = PackageStatus.ACTIVE;

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }
}
