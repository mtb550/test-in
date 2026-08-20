package org.testin.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestCaseExecutionListener;
import org.testin.services.Services;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewPanelExecutionSubscriber {
    // Written from the TestNG runner thread and read from the EDT.
    private final @NotNull Map<String, UUID> uuidToDtoId = new ConcurrentHashMap<>();
    private final @NotNull ProjectIndexer indexer;
    /**
     * The case the runner is on, empty until one reports itself. Volatile: it is
     * written from the TestNG thread and read from the EDT.
     */
    private volatile @NotNull Optional<UUID> runningDtoId = Optional.empty();

    public ViewPanelExecutionSubscriber(final @NotNull Project p, final @NotNull ViewPanel viewPanel) {
        this.indexer = Services.getInstance(p, ProjectIndexer.class);

        p.getMessageBus().connect(viewPanel).subscribe(TestCaseExecutionListener.TOPIC, new TestCaseExecutionListener() {
            @Override
            public void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error) {
                Logger.debug("ViewPanel subscription fired: testName='" + testName + "', status='" + status + "'");

                final String reported = Objects.toString(error, "");

                final Optional<TestCaseDto> byId = parseUuid(testName).flatMap(indexer::findTestCase);

                byId.ifPresent(tc -> {
                    Logger.debug("  ID match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                    tc.setTempStatus(status);
                    tc.setTempError(reported);
                    runningDtoId = Optional.of(tc.getId());
                });

                final Optional<TestCaseDto> found = byId.isPresent()
                        ? byId
                        : Optional.ofNullable(uuidToDtoId.get(testName)).flatMap(indexer::findTestCase);

                if (byId.isEmpty()) {
                    found.ifPresent(tc -> {
                        Logger.debug("  UUID map match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                        tc.setTempStatus(status);
                        tc.setTempError(reported);
                    });
                }

                final boolean updated = found.isPresent();

                // Snapshotted: the field is volatile, so re-reading it after the
                // check could see a different value.
                final Optional<UUID> runningId = runningDtoId;
                if (!updated && status == RunStatus.RUNNING && !uuidToDtoId.containsKey(testName)) {
                    runningId.flatMap(indexer::findTestCase).ifPresent(tc -> {
                        Logger.debug("  Mapping UUID='" + testName + "' -> DTO id='" + tc.getId() + "' desc='" + tc.getDescription() + "'");
                        uuidToDtoId.put(testName, tc.getId());
                    });
                }

                // This callback arrives on the TestNG execution thread;
                // Swing components may only be touched on the EDT.
                if (updated)
                    ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
            }

            /**
             * The test name as an id, when that is what it is.
             */
            private @NotNull Optional<UUID> parseUuid(final @NotNull String s) {
                try {
                    return Optional.of(UUID.fromString(s));
                } catch (final IllegalArgumentException notAnId) {
                    return Optional.empty();
                }
            }
        });
    }
}
