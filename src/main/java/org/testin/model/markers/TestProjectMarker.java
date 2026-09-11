package org.testin.model.markers;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;
import org.testin.model.ProjectStatus;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestProjectMarker extends AbstractMarker {
    @NonNull
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Override
    public @NotNull NodeStatus status() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> statuses() {
        return List.of(ProjectStatus.values());
    }

    @Override
    public void applyStatus(final @NotNull NodeStatus status) {
        setStatus((ProjectStatus) status);
    }
}
