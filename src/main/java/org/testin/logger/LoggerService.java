package org.testin.logger;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service(Service.Level.APP)
public final class LoggerService implements Disposable {

    private static final long MAX_LOG_SIZE = 5L * 1024 * 1024;
    /**
     * Not a log line. The queue holds Object so this can be one: a String
     * sentinel would have to be identity-compared against text a tester could
     * legitimately write.
     */
    private static final Object SHUTDOWN = new Object();
    private final BlockingQueue<Object> logQueue = new ArrayBlockingQueue<>(10000);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    // The IDE's log directory - beside idea.log, so Help -> Show Log in
    // Explorer finds it and Collect Logs and Diagnostic Data bundles it.
    // Resolved once; the location never depends on any open project.
    private final Path logFile = Path.of(PathManager.getLogPath(), "testin.log");
    private volatile boolean isRunning = true;
    private volatile Level currentLogLevel = Level.DISABLED;
    private Thread writerThread;

    public LoggerService() {
        startWriterThread();
    }

    public void setLogLevel(final @NotNull Level level) {
        this.currentLogLevel = level;
    }

    private void startWriterThread() {
        writerThread = new Thread(this::writeLoop, "Testin-Async-Logger");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    private void writeLoop() {
        try {
            long written;
            try {
                written = Files.size(logFile);
            } catch (final NoSuchFileException ex) {
                written = 0;
            }
            BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            try {
                while (isRunning || !logQueue.isEmpty()) {

                    final Object taken = logQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (taken == null) {
                        writer.flush();
                        continue;
                    }

                    // The wake-up from dispose, not a line to write.
                    if (!(taken instanceof String message)) continue;

                    writer.write(message);
                    writer.newLine();
                    written += message.length() + 1;

                    if (written >= MAX_LOG_SIZE) {
                        final @Nullable BufferedWriter rolled = rollOver(writer);

                        // The roll-over closed the old writer before it failed, so
                        // there is nothing left to write into. Stop draining rather
                        // than write into a closed stream.
                        if (rolled == null) return;

                        writer = rolled;
                        written = 0;
                    }
                }
            } finally {
                writer.close();
            }
        } catch (final IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The log survives shutdown, so it is capped instead: on exceeding the
     * limit the file rolls to a single .1 backup and starts fresh.
     * <p>
     * Null when the roll-over failed. The failure cannot be logged — this is the
     * thread that drains the log queue, and a Logger call here would report the
     * logger's own failure into the logger. It is the one catch in the plugin
     * that stays silent, and it says so rather than declaring {@code throws} and
     * letting the writeLoop's catch-all decide.
     */
    private @Nullable BufferedWriter rollOver(final @NotNull BufferedWriter writer) {
        try {
            writer.close();
            Files.move(logFile, logFile.resolveSibling("testin.log.1"), StandardCopyOption.REPLACE_EXISTING);
            return Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException ex) {
            return null;
        }
    }

    public void log(@NotNull Level level, @NotNull String callerClass, @NotNull String message) {

        if (!isRunning || currentLogLevel == Level.DISABLED || level.priority < currentLogLevel.priority) return;

        String formattedMessage = "[" + LocalDateTime.now().format(formatter) + "] " + "[" + level.paddedName + "] " + "[" + callerClass + "] " + message;

        if (!logQueue.offer(formattedMessage))
            Logger.error("Testin Logger queue full! Dropped log: " + message);

    }

    @Override
    public void dispose() {
        // Never deletes the log - it must survive shutdown so users can attach
        // it to bug reports. Only stop accepting, let the writer drain the
        // queue, and give it a bounded moment to flush the tail.
        isRunning = false;

        // Wakes the writer immediately. Without it the thread sits out the rest
        // of its 500ms poll before noticing the flag, and every quit pays for it.
        //
        // The result is deliberately ignored: offer only fails when the queue is
        // full, and a full queue means the writer is already draining rather than
        // waiting on poll - exactly the case where the wake-up is not needed.
        //noinspection ResultOfMethodCallIgnored
        logQueue.offer(SHUTDOWN);

        if (writerThread != null) {
            try {
                writerThread.join(2000);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (writerThread.isAlive()) writerThread.interrupt();
        }
    }
}
