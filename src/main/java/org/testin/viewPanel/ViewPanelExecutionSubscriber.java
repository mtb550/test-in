package org.testin.viewPanel;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.broadcasts.listeners.ITestCaseExecutionListener;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViewPanelExecutionSubscriber {
    private final Map<String, UUID> uuidToDtoId = new HashMap<>();
    private final ProjectIndexer indexer;
    private UUID runningDtoId = null;

    public ViewPanelExecutionSubscriber(final @NotNull ViewPanel viewPanel) {
        Project project = viewPanel.getProject();
        this.indexer = Services.getInstance(project, ProjectIndexer.class);

        project.getMessageBus().connect(viewPanel).subscribe(ITestCaseExecutionListener.TOPIC, new ITestCaseExecutionListener() {
            @Override
            public void onStatusChanged(@NotNull String testName, @NotNull String status, String error) {
                Log.debug("ViewPanel subscription fired: testName='" + testName + "', status='" + status + "'");

                boolean updated = false;

                final UUID testUuid = parseUuid(testName);
                if (testUuid != null) {
                    final TestCaseDto tc = indexer.getTestCaseById(testUuid);
                    Log.debug("  ID match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                    tc.setTempStatus(status);
                    tc.setTempError(error != null ? error : "");
                    runningDtoId = tc.getId();
                    updated = true;
                }

                if (!updated) {
                    final UUID dtoId = uuidToDtoId.get(testName);
                    if (dtoId != null) {
                        final TestCaseDto tc = indexer.getTestCaseById(dtoId);
                        Log.debug("  UUID map match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                        tc.setTempStatus(status);
                        tc.setTempError(error != null ? error : "");
                        updated = true;
                    }
                }

                if (!updated && "RUNNING".equals(status) && runningDtoId != null && !uuidToDtoId.containsKey(testName)) {
                    final TestCaseDto tc = indexer.getTestCaseById(runningDtoId);
                    Log.debug("  Mapping UUID='" + testName + "' -> DTO id='" + tc.getId() + "' desc='" + tc.getDescription() + "'");
                    uuidToDtoId.put(testName, tc.getId());
                }

                if (updated)
                    viewPanel.refreshCurrentView();
            }

            private UUID parseUuid(final String s) {
                try {
                    return UUID.fromString(s);
                } catch (final IllegalArgumentException ex) {
                    return null;
                }
            }
        });
    }
}
