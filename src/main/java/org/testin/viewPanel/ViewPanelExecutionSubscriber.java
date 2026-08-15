package org.testin.viewPanel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.RunStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.listeners.TestCaseExecutionListener;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewPanelExecutionSubscriber {
    // Written from the TestNG runner thread and read from the EDT.
    private final @NotNull Map<String, UUID> uuidToDtoId = new ConcurrentHashMap<>();
    private final @NotNull ProjectIndexer indexer;
    private volatile @Nullable UUID runningDtoId = null;

    public ViewPanelExecutionSubscriber(final @NotNull Project p, final @NotNull ViewPanel viewPanel) {
        this.indexer = Services.getInstance(p, ProjectIndexer.class);

        p.getMessageBus().connect(viewPanel).subscribe(TestCaseExecutionListener.TOPIC, new TestCaseExecutionListener() {
            @Override
            public void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error) {
                Logger.debug("ViewPanel subscription fired: testName='" + testName + "', status='" + status + "'");

                boolean updated = false;

                final UUID testUuid = parseUuid(testName);
                if (testUuid != null) {
                    final TestCaseDto tc = indexer.getTestCaseById(testUuid);

                    if (tc == null) return;

                    Logger.debug("  ID match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                    tc.setTempStatus(status);
                    tc.setTempError(error != null ? error : "");
                    runningDtoId = tc.getId();
                    updated = true;
                }

                if (!updated) {
                    final UUID dtoId = uuidToDtoId.get(testName);
                    if (dtoId != null) {
                        final TestCaseDto tc = indexer.getTestCaseById(dtoId);

                        if (tc == null) return;

                        Logger.debug("  UUID map match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                        tc.setTempStatus(status);
                        tc.setTempError(error != null ? error : "");
                        updated = true;
                    }
                }

                // Snapshotted: the field is volatile, so re-reading it after the
                // check could see a different value.
                final UUID runningId = runningDtoId;
                if (!updated && status == RunStatus.RUNNING && runningId != null && !uuidToDtoId.containsKey(testName)) {
                    final TestCaseDto tc = indexer.getTestCaseById(runningId);
                    if (tc == null) return;

                    Logger.debug("  Mapping UUID='" + testName + "' -> DTO id='" + tc.getId() + "' desc='" + tc.getDescription() + "'");
                    uuidToDtoId.put(testName, tc.getId());
                }

                // This callback arrives on the TestNG execution thread;
                // Swing components may only be touched on the EDT.
                if (updated)
                    ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
            }

            private @Nullable UUID parseUuid(final @NotNull String s) {
                try {
                    return UUID.fromString(s);
                } catch (final IllegalArgumentException ex) {
                    return null;
                }
            }
        });
    }
}
