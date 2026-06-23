package org.testin.editorPanel.runEditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.services.Services;

import java.nio.file.Path;
import java.util.*;

public class RunSessionCache {

    private final TestRunDto tr;
    @Getter
    private final Path testRunPath;

    private final List<TestCaseDto> loadedItems = Collections.synchronizedList(new ArrayList<>());

    private final Set<String> loadedModules = Collections.synchronizedSet(new HashSet<>());

    @Setter
    private ICacheListener listener;

    private volatile boolean isDisposed = false;

    public RunSessionCache(final TestRunDto tr, final Path testRunPath) {
        this.tr = tr;
        this.testRunPath = testRunPath;
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

    public void startLoadingAsync(final @NotNull Project project) {
        if (tr == null || tr.getResults().isEmpty()) {
            notifyLoadComplete(Collections.emptyList());
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

            indexer.awaitIndexing();

            if (isDisposed) return;

            final List<TestCaseDto> batch = new ArrayList<>();
            final int BATCH_SIZE = 5;

            final Set<UUID> targetIds = new HashSet<>();
            for (final TestRunItems item : tr.getResults()) {
                targetIds.add(item.getId());
            }

            for (final UUID id : targetIds) {
                if (isDisposed) return;

                final TestCaseDto tc = indexer.getTestCaseById(id);
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
            }

            if (!batch.isEmpty()) {
                notifyItemsLoaded(batch);
            }
            notifyLoadComplete(getLoadedItems());
        });
    }

    private void notifyItemsLoaded(final List<TestCaseDto> items) {
        Optional.ofNullable(listener).filter(l -> !isDisposed).ifPresent(l ->
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed) l.onItemsLoaded(items);
                })
        );
    }

    private void notifyLoadComplete(final List<TestCaseDto> items) {
        Optional.ofNullable(listener).filter(l -> !isDisposed).ifPresent(listener ->
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!isDisposed) listener.onLoadComplete(items);
                })
        );
    }

    public interface ICacheListener {
        void onItemsLoaded(final List<TestCaseDto> items);

        void onLoadComplete(final List<TestCaseDto> allItems);
    }
}