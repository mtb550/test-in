package org.testin.editor.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestCaseExecutionListener;
import org.testin.services.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TestCaseExecutionSubscriber {
    private final @NotNull Map<String, UUID> uuidToDtoId = new HashMap<>();
    private final @NotNull ProjectIndexer indexer;
    private @Nullable UUID runningDtoId = null;

    public TestCaseExecutionSubscriber(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list, final @NotNull Disposable parentDisposable) {
        this.indexer = Services.getInstance(p, ProjectIndexer.class);

        p.getMessageBus().connect(parentDisposable).subscribe(TestCaseExecutionListener.TOPIC, new TestCaseExecutionListener() {
            @Override
            public void onStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final String error) {
                // Today's publishers already fire on the EDT, but nothing
                // enforces that; hop like ViewPanelExecutionSubscriber does,
                // so the map, runningDtoId and the repaint stay EDT-confined.
                ApplicationManager.getApplication().invokeLater(() ->
                        handleStatusChanged(testName, status, Objects.toString(error, "")));
            }

            private void handleStatusChanged(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull String error) {
                Logger.debug("TestEditor subscription fired: testName='" + testName + "', status='" + status + "'");

                final Optional<TestCaseDto> byId = parseUuid(testName).flatMap(indexer::findTestCase);

                byId.ifPresent(tc -> {
                    Logger.debug("ID match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                    tc.setTempStatus(status);
                    tc.setTempError(error);
                    runningDtoId = tc.getId();
                });

                final Optional<TestCaseDto> byMapping = byId.isPresent()
                        ? byId
                        : Optional.ofNullable(uuidToDtoId.get(testName)).flatMap(indexer::findTestCase);

                if (byId.isEmpty()) {
                    byMapping.ifPresent(tc -> {
                        Logger.debug("  UUID map match! desc='" + tc.getDescription() + "', setting tempStatus='" + status + "'");
                        tc.setTempStatus(status);
                        tc.setTempError(error);
                    });
                }

                final boolean updated = byMapping.isPresent();

                if (!updated && status == RunStatus.RUNNING && runningDtoId != null && !uuidToDtoId.containsKey(testName)) {
                    indexer.findTestCase(runningDtoId).ifPresent(tc -> {
                        Logger.debug("  Mapping UUID='" + testName + "' -> DTO id='" + tc.getId() + "' desc='" + tc.getDescription() + "'");
                        uuidToDtoId.put(testName, tc.getId());
                    });
                }

                if (updated)
                    list.repaint();
            }

            /**
             * The test name as an id, when that is what it is: TestNG names a
             * method, and only a Testin-generated one is an id.
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