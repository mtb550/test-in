package org.testin.editorPanel.testCaseEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class TestSessionCache {

    private final Project project;
    private final Path directoryPath;

    private final List<TestCaseDto> loadedItems = Collections.synchronizedList(new ArrayList<>());

    private final Set<String> loadedModules = Collections.synchronizedSet(new HashSet<>());

    @Setter
    private ICacheListener listener;

    private volatile boolean isDisposed = false;

    public TestSessionCache(final @NotNull Project project, final Path directoryPath) {
        this.project = project;
        this.directoryPath = directoryPath;
    }

    public List<TestCaseDto> getLoadedItems() {
        return new ArrayList<>(loadedItems);
    }

    public Set<String> getLoadedModules() {
        return new HashSet<>(loadedModules);
    }

    public void dispose() {
        isDisposed = true;
        loadedItems.clear();
        loadedModules.clear();
        listener = null;
    }

    public void startLoadingAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final List<TestCaseDto> batch = new ArrayList<>();
            final int BATCH_SIZE = 5;

            try (final Stream<Path> paths = Files.list(directoryPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(filePath -> {
                            if (isDisposed) return;

                            try {
                                final TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
                                    loadedItems.add(tc);
                                    batch.add(tc);

                                    final String moduleName = tc.getModule();
                                    if (!moduleName.trim().isEmpty()) loadedModules.add(moduleName.trim());

                                    if (batch.size() >= BATCH_SIZE) {
                                        final List<TestCaseDto> itemsToSend = new ArrayList<>(batch);
                                        batch.clear();
                                        notifyItemsLoaded(itemsToSend);
                                    }
                                }
                            } catch (final Exception ex) {
                                Log.error("Unable to read test case file: " + filePath.toAbsolutePath());
                                Log.error("Exception: " + ex.getMessage());
                            }
                        });

                if (!batch.isEmpty()) {
                    notifyItemsLoaded(batch);
                }

            } catch (final Exception e) {
                if (!isDisposed)
                    Log.error("Exception: " + e.getMessage());
            }

            notifyLoadComplete();
        });
    }

    private void notifyItemsLoaded(final List<TestCaseDto> items) {
        Optional.ofNullable(listener)
                .filter(l -> !isDisposed)
                .ifPresent(l -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed) l.onItemsLoaded(items);
                }));
    }

    private void notifyLoadComplete() {
        Optional.ofNullable(listener)
                .filter(l -> !isDisposed)
                .ifPresent(l -> ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed) l.onLoadComplete(getLoadedItems());
                }));
    }

    public interface ICacheListener {
        void onItemsLoaded(final List<TestCaseDto> items);

        void onLoadComplete(final List<TestCaseDto> allItems);
    }
}