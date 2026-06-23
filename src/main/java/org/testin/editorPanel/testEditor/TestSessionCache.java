package org.testin.editorPanel.testEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.*;

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
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

            indexer.awaitIndexing();

            if (isDisposed) return;

            final List<TestCaseDto> allCases = indexer.getTestCasesForTestSet(directoryPath);

            for (final TestCaseDto tc : allCases) {
                final String moduleName = tc.getModule();
                if (!moduleName.trim().isEmpty()) loadedModules.add(moduleName.trim());
            }

            final int BATCH_SIZE = 5;
            final List<TestCaseDto> batch = new ArrayList<>();
            for (final TestCaseDto tc : allCases) {
                if (isDisposed) return;
                loadedItems.add(tc);
                batch.add(tc);

                if (batch.size() >= BATCH_SIZE) {
                    final List<TestCaseDto> itemsToSend = new ArrayList<>(batch);
                    batch.clear();
                    notifyItemsLoaded(itemsToSend);
                }
            }

            if (!batch.isEmpty()) {
                notifyItemsLoaded(batch);
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