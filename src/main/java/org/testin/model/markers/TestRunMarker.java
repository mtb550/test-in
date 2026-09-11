package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunStatus;


@Setter
@Getter
@Accessors(chain = true)
@ToString(callSuper = true)
public class TestRunMarker extends AbstractMarker {
    @NonNull
    private TestRunStatus status = TestRunStatus.CREATED;

    /**
     * The one marker that shows a status the tester cannot set.
     * <p>
     * A run's status is the run's own account of itself - created, executed -
     * moved by running it, not by a menu entry. So {@link TestRunStatus} is not
     * a {@link org.testin.model.NodeStatus} and this answers the label directly
     * rather than through {@code status()} (#110).
     */
    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }

}
