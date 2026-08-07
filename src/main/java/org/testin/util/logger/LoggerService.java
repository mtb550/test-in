package org.testin.util.logger;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.APP)
public final class LoggerService implements Disposable {

    private final BlockingQueue<String> logQueue = new ArrayBlockingQueue<>(10000);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private volatile boolean isRunning = true;

    private volatile Logger.Level currentLogLevel = Logger.Level.DISABLED;

    private Thread writerThread;

    public LoggerService() {
        startWriterThread();
    }

    public void setLogLevel(@NotNull Logger.Level level) {
        this.currentLogLevel = level;
    }

    private void startWriterThread() {
        writerThread = new Thread(() -> {
            Path logFile = getLogFile();
            if (logFile == null) return;

            try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                while (isRunning || !logQueue.isEmpty()) {

                    String message = logQueue.poll(500, TimeUnit.MILLISECONDS);

                    if (message != null) {
                        writer.write(message);
                        writer.newLine();
                    } else {
                        writer.flush();
                    }
                }
            } catch (final IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "Testin-Async-Logger");

        writerThread.setDaemon(true);
        writerThread.start();
    }

    private Path getLogFile() {
        try {
            Project p = Logger.getProject();
            Path projectDir;
            if (p != null && p.getBasePath() != null) {
                projectDir = Path.of(p.getBasePath());
            } else {
                projectDir = Path.of("").toAbsolutePath();
            }
            if (!Files.exists(projectDir)) Files.createDirectories(projectDir);
            return projectDir.resolve("testin.log");
        } catch (final Exception ex) {
            Logger.error("Failed to initialize log file path: " + ex.getMessage());
            return null;
        }
    }

    public void log(@NotNull Logger.Level level, @NotNull String callerClass, @NotNull String message) {

        if (!isRunning || currentLogLevel == Logger.Level.DISABLED || level.priority < currentLogLevel.priority) return;

        String formattedMessage = "[" + LocalDateTime.now().format(formatter) + "] " + "[" + level.paddedName + "] " + "[" + callerClass + "] " + message;

        if (!logQueue.offer(formattedMessage))
            Logger.error("Testin Logger queue full! Dropped log: " + message);

    }

    @Override
    public void dispose() {
        isRunning = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }

        try {
            Path logFile = getLogFile();
            if (logFile != null && Files.exists(logFile))
                Files.delete(logFile);

        } catch (final Exception ex) {
            Logger.error("Failed to delete log file: " + ex.getMessage());
        }
    }
}
