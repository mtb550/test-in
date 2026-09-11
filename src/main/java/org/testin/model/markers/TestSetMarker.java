package org.testin.model.markers;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.NodeStatus;
import org.testin.model.TestSetStatus;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestSetMarker extends AbstractMarker {
    /**
     * Deprecated test sets keep their cases and their run history; they stop
     * being offered when a new run is configured (#68).
     */
    @NonNull
    private TestSetStatus status = TestSetStatus.ACTIVE;

    @Override
    public @NotNull NodeStatus status() {
        return status;
    }

    @Override
    public @NotNull List<NodeStatus> statuses() {
        return List.of(TestSetStatus.values());
    }

    @Override
    public void applyStatus(final @NotNull NodeStatus status) {
        setStatus((TestSetStatus) status);
    }
}
