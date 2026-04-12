package testGit.editorPanel.testCaseEditor;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TestSessionCache {

    private final Path directoryPath;
    private final List<TestCaseDto> loadedItems = Collections.synchronizedList(new ArrayList<>());
    @Setter
    private CacheListener listener;

    private volatile boolean isDisposed = false;

    public TestSessionCache(final Path directoryPath) {
        this.directoryPath = directoryPath;
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
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final List<TestCaseDto> batch = new ArrayList<>();
            final int BATCH_SIZE = 5;

            try (final Stream<Path> paths = Files.list(directoryPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(filePath -> {
                            if (isDisposed) return;

                            try {
                                final TestCaseDto tc = Config.getMapper().readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
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

                if (!batch.isEmpty()) {
                    notifyItemsLoaded(batch);
                }

            } catch (final Exception e) {
                if (!isDisposed) e.printStackTrace(System.out);
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

    public interface CacheListener {
        void onItemsLoaded(final List<TestCaseDto> items);

        void onLoadComplete(final List<TestCaseDto> allItems);
    }
}