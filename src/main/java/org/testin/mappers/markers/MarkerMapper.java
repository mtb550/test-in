package org.testin.mappers.markers;

import com.intellij.openapi.components.Service;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestRunStatus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class MarkerMapper {

    public @NotNull TestSetMarker setTestSetMarker() {
        return TestSetMarker.builder()
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }

    public @NotNull TestCasesMainDirectoryMarker setTestCasesMainDirectoryMarker() {
        return TestCasesMainDirectoryMarker.builder()
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }

    public @NotNull TestRunsMainDirectoryMarker setTestRunsMainDirectoryMarker() {
        return TestRunsMainDirectoryMarker.builder()
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }

    public @NotNull TestSetPackageMarker setTestSetPackageMarker() {
        return TestSetPackageMarker.builder()
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }

    public @NotNull TestRunMarker setTestRunMarker() {
        return TestRunMarker.builder()
                .status(TestRunStatus.CREATED)
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }

    public @NotNull TestRunPackageMarker setTestRunPackageMarker() {
        return TestRunPackageMarker.builder()
                .createdBy(System.getProperty("user.name", ""))
                .build();
    }
}
