package org.testin.editorPanel.testRunEditor;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import org.testin.pojo.TestRunItems;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.util.Mapper;
import org.testin.util.Tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RunSessionCache {

    private final TestRunDto tr;

    private final List<TestCaseDto> loadedItems = Collections.synchronizedList(new ArrayList<>());

    private final Set<String> loadedModules = Collections.synchronizedSet(new HashSet<>());

    @Setter
    private ICacheListener listener;

    private volatile boolean isDisposed = false;

    public RunSessionCache(final TestRunDto tr) {
        this.tr = tr;
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
        if (tr == null || tr.getResults().isEmpty()) {
            notifyLoadComplete(Collections.emptyList());
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final List<TestCaseDto> batch = new ArrayList<>();
            final int BATCH_SIZE = 5;

            final Map<List<String>, List<UUID>> pathMap = new HashMap<>();
            for (TestRunItems item : tr.getResults()) {
                pathMap.computeIfAbsent(item.getPath(), k -> new ArrayList<>()).add(item.getId());
            }

            for (final Map.Entry<List<String>, List<UUID>> entry : pathMap.entrySet()) {
                if (isDisposed) break;

                final List<String> pathSegments = entry.getKey();
                final List<UUID> targetIds = entry.getValue();

                final Path dirPath = Tools.getInstance().buildLocalPathFromList(pathSegments);

                if (dirPath == null || !Files.exists(dirPath) || targetIds.isEmpty()) {
                    System.err.println("[WARNING] directory path not found: " + dirPath);
                    System.err.println("[WARNING] path not found: " + pathSegments);
                    continue;
                }

                final Set<UUID> idsToFind = new HashSet<>(targetIds);

                try (final Stream<Path> paths = Files.list(dirPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".json"))
                            .forEach(filePath -> {
                                if (isDisposed) return;

                                try {
                                    final TestCaseDto tc = Mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                    if (tc != null && idsToFind.contains(tc.getId())) {
                                        loadedItems.add(tc);
                                        batch.add(tc);

                                        if (batch.size() >= BATCH_SIZE) {
                                            final List<TestCaseDto> itemsToSend = new ArrayList<>(batch);
                                            batch.clear();
                                            notifyItemsLoaded(itemsToSend);
                                        }
                                    }
                                } catch (final Exception ignored) {
                                }
                            });
                } catch (final Exception ex) {
                    System.err.println("Unable to read test case file: " + dirPath.toAbsolutePath());
                    ex.printStackTrace(System.err);
                    if (!isDisposed) System.err.println("Failed to load cases from: " + dirPath);
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