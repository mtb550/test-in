/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.runner;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.KillableProcess;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class TestNGExecution implements Disposable {
    private final @NotNull Project p;

    private final @NotNull RunRegistry registry = new RunRegistry();

    private final @NotNull Map<ProcessHandler, String> live = new ConcurrentHashMap<>();

    public TestNGExecution(final @NotNull Project p) {
        this.p = p;

        p.getMessageBus().connect(this).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler) {
                final @NotNull String runName = env.getRunProfile().getName();
                if (registry.launchedHere(runName)) live.put(handler, runName);
            }

            @Override
            public void processTerminated(final @NotNull String executorId, final @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler, final int exitCode) {
                live.remove(handler);
                ended(env);
            }

            @Override
            public void processNotStarted(final @NotNull String executorId, final @NotNull ExecutionEnvironment env) {
                ended(env);
            }
        });
    }

    @Override
    public void dispose() {
    }

    // UC-CODEGEN-008, Rule-CODEGEN-034
    public void starting(final @NotNull TestCaseDto tc) {
        registry.starting(tc.getId());

        TestCaseExecutionListener.broadcast(p, key(tc.getId()), RunStatus.RUNNING, Duration.ZERO, Failure.NONE);
    }

    public @NotNull List<TestCaseDto> stillWanted(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().filter(tc -> registry.take(tc.getId())).toList();
    }

    // UC-CODEGEN-008, Rule-CODEGEN-076
    public @NotNull String freeRunName(final @NotNull String wanted) {
        return registry.freeName(wanted);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-031
    public void launch(final @NotNull List<TestCaseDto> cases, final @NotNull RunnerAndConfigurationSettings settings) {
        registry.launched(cases.stream().map(TestCaseDto::getId).toList(), settings.getName());

        Logger.info("Starting " + settings.getName() + " with " + cases.size() + " test case(s)");
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    public void notStarting(final @NotNull TestCaseDto tc) {
        registry.notStarting(tc.getId());

        TestCaseExecutionListener.broadcast(p, key(tc.getId()), RunStatus.IDLE, Duration.ZERO, Failure.NONE);
    }

    // UC-CODEGEN-008
    public void noGeneratedCode(final @NotNull TestCaseDto tc) {
        Logger.warn("Not running '" + tc.getDescription() + "': it has no generated code");
        notStarting(tc);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-033, Rule-CODEGEN-074, Rule-CODEGEN-034
    public void started(final @NotNull List<TestCaseDto> running, final @NotNull List<TestCaseDto> withoutCode) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        notifier.softShowCounted(p, RunStatus.RUNNING.getBadge().label(), running.size());
        if (withoutCode.isEmpty()) return;

        final boolean one = withoutCode.size() == 1;
        notifier.softRefuse(p, one ? Refused.NO_GENERATED_CODE : Refused.NO_GENERATED_CODE_COUNTED,
                one ? withoutCode.getFirst().getDescription() : String.valueOf(withoutCode.size()));
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    public boolean isStopped(final @NotNull TestCaseDto tc) {
        return registry.isStopped(tc.getId());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-036
    public boolean isRunning(final @NotNull UUID id) {
        return registry.isRunning(id);
    }

    public @NotNull RunStatus statusOf(final @NotNull TestCaseDto tc) {
        return registry.statusOf(tc.getId());
    }

    // UC-CODEGEN-008
    void reported(final @NotNull UUID id, final @NotNull RunStatus status) {
        registry.reported(id, status);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stop(final @NotNull List<TestCaseDto> cases) {
        return stopCases(cases.stream().map(TestCaseDto::getId).toList());
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    public int stopCases(final @NotNull Collection<UUID> ids) {
        final @NotNull RunRegistry.Stop stop = registry.stopping(List.copyOf(ids));
        if (stop.cases().isEmpty()) return 0;

        final @NotNull Map<ProcessHandler, String> theirs = running(stop.runs());
        Logger.info("Stopping " + stop.cases().size() + " test case(s) in " + stop.runs().size()
                + " run(s): " + theirs.size() + " had reached a process");

        theirs.forEach(this::kill);
        stop.cases().forEach(id -> TestCaseExecutionListener.broadcast(p, key(id), RunStatus.IDLE, Duration.ZERO, Failure.NONE));

        return stop.cases().size();
    }

    private void ended(final @NotNull ExecutionEnvironment env) {
        final @NotNull String runName = env.getRunProfile().getName();
        final @NotNull List<UUID> abandoned = registry.ended(runName);
        if (abandoned.isEmpty()) return;

        Logger.info("'" + runName + "' ended with " + abandoned.size() + " test case(s) that never reported");
        abandoned.forEach(id -> TestCaseExecutionListener.broadcast(p, key(id), RunStatus.IDLE, Duration.ZERO, Failure.NONE));
    }

    private void kill(final @NotNull ProcessHandler handler, final @NotNull String runName) {
        handler.putUserData(ProcessHandler.TERMINATION_REQUESTED, Boolean.TRUE);

        if (handler instanceof KillableProcess killable && killable.canKillProcess()) {
            Logger.info("Killing '" + runName + "'");
            killable.killProcess();

        } else {
            Logger.info("Destroying '" + runName + "', which cannot be killed");
            handler.destroyProcess();
        }
    }

    private @NotNull Map<ProcessHandler, String> running(final @NotNull Set<String> names) {
        return live.entrySet().stream()
                .filter(one -> names.contains(one.getValue()) && !one.getKey().isProcessTerminated())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static @NotNull String key(final @NotNull UUID id) {
        return id.toString().toLowerCase();
    }
}
