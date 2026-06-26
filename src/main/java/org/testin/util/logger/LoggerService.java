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

    private volatile Log.Level currentLogLevel = Log.Level.DISABLED;

    private Thread writerThread;

    public LoggerService() {
        startWriterThread();
    }

    public void setLogLevel(@NotNull Log.Level level) {
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
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Testin-Async-Logger");

        writerThread.setDaemon(true);
        writerThread.start();
    }

    private Path getLogFile() {
        try {
            Project project = Log.getProject();
            Path projectDir;
            if (project != null && project.getBasePath() != null) {
                projectDir = Path.of(project.getBasePath());
            } else {
                projectDir = Path.of("").toAbsolutePath();
            }
            if (!Files.exists(projectDir)) Files.createDirectories(projectDir);
            return projectDir.resolve("testin.log");
        } catch (Exception e) {
            Log.error("Failed to initialize log file path: " + e.getMessage());
            return null;
        }
    }

    public void log(@NotNull Log.Level level, @NotNull String callerClass, @NotNull String message) {

        if (!isRunning || currentLogLevel == Log.Level.DISABLED || level.priority < currentLogLevel.priority) return;

        String formattedMessage = "[" + LocalDateTime.now().format(formatter) + "] " +
                "[" + level.paddedName + "] " +
                "[" + callerClass + "] " + message;

        if (!logQueue.offer(formattedMessage)) {
            Log.error("Testin Logger queue full! Dropped log: " + message);
        }
    }

    @Override
    public void dispose() {
        isRunning = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }

        try {
            Path logFile = getLogFile();
            if (logFile != null && Files.exists(logFile)) {
                Files.delete(logFile);
            }
        } catch (Exception ignored) {
        }
    }
}
