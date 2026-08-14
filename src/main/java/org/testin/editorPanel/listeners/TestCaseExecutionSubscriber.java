package org.testin.editorPanel.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.RunStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.listeners.ITestCaseExecutionListener;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestCaseExecutionSubscriber {
    private final @NotNull Map<String, UUID> uuidToDtoId = new HashMap<>();
    private final @NotNull ProjectIndexer indexer;
    private @Nullable UUID runningDtoId = null;

    public TestCaseExecutionSubscriber(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list, final @NotNull Disposable parentDisposable) {
        this.indexer = Services.getInstance(p, ProjectIndexer.class);

        p.getMessageBus().connect(parentDisposable).subscribe(ITestCaseExecutionListener.TOPIC, new ITestCaseExecutionListener() {
            @Override
            public void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error) {
                // Today's publishers already fire on the EDT, but nothing
                // enforces that; hop like ViewPanelExecutionSubscriber does,
                // so the map, runningDtoId and the repaint stay EDT-confined.
                ApplicationManager.getApplication().invokeLater(() -> handleStatusChanged(testName, status, error));
            }

            private void handleStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final @Nullable String error) {
                Logger.debug("TestEditor subscription fired: testName='" + testName + "', status='" + status + "'");

                boolean updated = false;

                final UUID testUuid = parseUuid(testName);
                if (testUuid != null) {
                    final TestCaseDto tc = indexer.getTestCaseById(testUuid);
                    if (tc != null) {
                        Logger.debug("ID match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                        tc.setTempStatus(status);
                        tc.setTempError(error != null ? error : "");
                        runningDtoId = tc.getId();
                        updated = true;
                    }
                }

                if (!updated) {
                    final UUID dtoId = uuidToDtoId.get(testName);
                    if (dtoId != null) {
                        final TestCaseDto tc = indexer.getTestCaseById(dtoId);
                        if (tc != null) {
                            Logger.debug("  UUID map match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                            tc.setTempStatus(status);
                            tc.setTempError(error != null ? error : "");
                            updated = true;
                        }
                    }
                }

                if (!updated && status == RunStatus.RUNNING && runningDtoId != null && !uuidToDtoId.containsKey(testName)) {
                    final TestCaseDto tc = indexer.getTestCaseById(runningDtoId);
                    if (tc != null) {
                        Logger.debug("  Mapping UUID='" + testName + "' -> DTO id='" + tc.getId() + "' desc='" + tc.getDescription() + "'");
                        uuidToDtoId.put(testName, tc.getId());
                    }
                }

                if (updated)
                    list.repaint();
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