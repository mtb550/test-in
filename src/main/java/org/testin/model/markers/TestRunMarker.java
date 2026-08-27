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

    @JsonIgnore
    @Override
    public @NotNull String getStatusLabel() {
        return status.getLabel();
    }

}
