package org.testin.mappers.markers;

import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestRunStatus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class MarkerMapper {

    public @NotNull TestRunMarker setTestRunMarker() {
        return TestRunMarker.builder()
                .status(TestRunStatus.CREATED)
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }
}
