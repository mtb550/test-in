package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ProjectStatus;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestProjectMarker extends AbstractMarker {
    @NonNull
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }
}
