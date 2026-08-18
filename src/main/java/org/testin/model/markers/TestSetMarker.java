package org.testin.model.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestSetStatus;

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

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }
}
