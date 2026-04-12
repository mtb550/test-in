package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RunSessionCache {

    private final TestRunDto metadata;
    private final List<TestCaseDto> loadedItems = Collections.synchronizedList(new ArrayList<>());
    @Setter
    private CacheListener listener;

    private volatile boolean isDisposed = false;

    public RunSessionCache(final TestRunDto metadata) {
        this.metadata = metadata;
    }

    public List<TestCaseDto> getLoadedItems() {
        return new ArrayList<>(loadedItems);
    }

    public void dispose() {
        isDisposed = true;
        loadedItems.clear();
        listener = null;
    }

    public void startLoadingAsync() {
        if (metadata == null || metadata.getTestCase() == null || metadata.getTestCase().isEmpty()) {
            notifyLoadComplete(Collections.emptyList());
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final List<TestCaseDto> batch = new ArrayList<>();
            final int BATCH_SIZE = 5;

            for (final TestRunDto.TestCase tcPathObj : metadata.getTestCase()) {
                if (isDisposed) break;

                final Path dirPath = tcPathObj.getPath();
                final List<UUID> targetIds = tcPathObj.getUuid();

                if (dirPath == null || !Files.exists(dirPath) || targetIds == null || targetIds.isEmpty()) {
                    continue;
                }

                final Set<UUID> idsToFind = new HashSet<>(targetIds);

                try (final Stream<Path> paths = Files.list(dirPath)) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".json"))
                            .forEach(filePath -> {
                                if (isDisposed) return;

                                try {
                                    final TestCaseDto tc = Config.getMapper().readValue(filePath.toFile(), TestCaseDto.class);
                                    if (tc != null && tc.getId() != null && idsToFind.contains(tc.getId())) {
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
                } catch (final Exception e) {
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

    public interface CacheListener {
        void onItemsLoaded(final List<TestCaseDto> items);

        void onLoadComplete(final List<TestCaseDto> allItems);
    }
}