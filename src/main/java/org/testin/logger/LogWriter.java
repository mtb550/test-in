package org.testin.logger;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.APP)
public final class LogWriter implements Disposable {

    private static final long MAX_LOG_SIZE = 5L * 1024 * 1024;

    /**
     * How long the writer gets to flush the tail of the log before it is
     * interrupted. Bounded so a stuck write cannot hold up a quit.
     */
    private static final int JOIN_TIMEOUT = 2000;

    /**
     * The IDE's log, and the only thing in this class that may be told anything.
     * Both callers are failures of the log itself, which cannot be reported
     * through the log. Named for what it is rather than imported, because
     * {@code Logger} in this package is Testin's own and two of them under one
     * name is how the wrong one gets called.
     */
    private static final com.intellij.openapi.diagnostic.@NotNull Logger IDE_LOG =
            com.intellij.openapi.diagnostic.Logger.getInstance(LogWriter.class);
    /**
     * Not a log line. The queue holds Object so this can be one: a String
     * sentinel would have to be identity-compared against text a tester could
     * legitimately write.
     */
    private static final @NotNull Object SHUTDOWN = new Object();
    private final @NotNull BlockingQueue<Object> logQueue = new ArrayBlockingQueue<>(10000);
    private final @NotNull DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    // The IDE's log directory - beside idea.log, so Help -> Show Log in
    // Explorer finds it and Collect Logs and Diagnostic Data bundles it.
    // Resolved once; the location never depends on any open project.
    private final @NotNull Path logFile = Path.of(PathManager.getLogPath(), "testin.log");
    private volatile boolean isRunning = true;
    private volatile @NotNull Level currentLogLevel = Level.DISABLED;
    /**
     * Empty until start() runs, which dispose has to allow for: a service the
     * IDE created and threw away without ever starting has no thread to join.
     */
    private @NotNull Optional<Thread> writerThread = Optional.empty();

    /**
     * Whether lines are being dropped right now, so the fact is said once per
     * episode rather than once per dropped line.
     */
    private final @NotNull AtomicBoolean dropping = new AtomicBoolean();

    public LogWriter() {
        startWriterThread();
    }

    // UC-SETTING-007, Rule-SETTING-024
    public void setLogLevel(final @NotNull Level level) {
        this.currentLogLevel = level;
    }

    private void startWriterThread() {
        final @NotNull Thread thread = new Thread(this::writeLoop, "Testin-Async-Logger");
        thread.setDaemon(true);
        thread.start();
        writerThread = Optional.of(thread);
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

                    final @NotNull Optional<Object> taken = Optional.ofNullable(logQueue.poll(500, TimeUnit.MILLISECONDS));
                    if (taken.isEmpty()) {
                        writer.flush();
                        continue;
                    }

                    // The wake-up from dispose, not a line to write.
                    if (!(taken.orElseThrow() instanceof String message)) continue;

                    writer.write(message);
                    writer.newLine();
                    written += message.length() + 1;

                    if (written >= MAX_LOG_SIZE) {
                        final @NotNull Optional<BufferedWriter> rolled = rollOver(writer);

                        // The roll-over closed the old writer before it failed, so
                        // there is nothing left to write into. Stop draining rather
                        // than write into a closed stream.
                        if (rolled.isEmpty()) return;

                        writer = rolled.get();
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
     * Empty when the roll-over failed. The failure cannot be logged — this is
     * the thread that drains the log queue, and a Logger call here would report
     * the logger's own failure into the logger. It is the one catch in the plugin
     * that stays silent, and it says so rather than declaring {@code throws} and
     * letting the writeLoop's catch-all decide.
     */
    private @NotNull Optional<BufferedWriter> rollOver(final @NotNull BufferedWriter writer) {
        try {
            writer.close();
            Files.move(logFile, logFile.resolveSibling("testin.log.1"), StandardCopyOption.REPLACE_EXISTING);
            return Optional.of(Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        } catch (final IOException ex) {
            return Optional.empty();
        }
    }

    public void log(final @NotNull Level level, final @NotNull String callerClass, final @NotNull String message) {
        if (!isRunning || currentLogLevel == Level.DISABLED || level.priority < currentLogLevel.priority) return;

        final @NotNull String formattedMessage = "[" + LocalDateTime.now().format(formatter) + "] " + "[" + level.paddedName + "] " + "[" + callerClass + "] " + message;

        if (logQueue.offer(formattedMessage)) {
            dropping.set(false);
            return;
        }

        sayTheQueueIsFull();
    }

    /**
     * That lines are being dropped, said where it can still be read.
     * <p>
     * This used to be {@code Logger.error("Testin Logger queue full!")}, which
     * came straight back into the method above, found the queue still full and
     * said it again - unbounded recursion, and a {@code StackOverflowError} on
     * whatever thread happened to be logging, inside indexing, a save or a run.
     * It was reachable rather than theoretical: {@link #writeLoop} exits on an
     * {@code IOException}, so a log directory that stops being writable kills
     * the drain and fills the queue behind it (#66, finding 65).
     * <p>
     * The rule {@link #rollOver} states is the rule here: the log's own failure
     * cannot be reported through the log. Said once per episode, because a
     * queue that is full is full for every line after it too, and re-armed by
     * the first line that gets in.
     */
    private void sayTheQueueIsFull() {
        if (dropping.getAndSet(true)) return;

        IDE_LOG.warn("Testin's log queue is full and lines are being dropped - the writer thread"
                + " has stopped draining it, so " + logFile + " ends where it stopped.");
    }

    @Override
    public void dispose() {
        final long started = System.nanoTime();

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

        boolean timedOut = false;

        if (writerThread.isPresent()) {
            final @NotNull Thread thread = writerThread.orElseThrow();
            try {
                thread.join(JOIN_TIMEOUT);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            timedOut = thread.isAlive();
            if (timedOut) thread.interrupt();
        }

        report(System.nanoTime() - started, timedOut);
    }

    /**
     * How long the shutdown took, in the IDE's own log (#292).
     * <p>
     * <b>One of the two places in the plugin that write to {@code idea.log}</b>,
     * the other being {@link #sayTheQueueIsFull}. Two reasons, and both are
     * about this method only. It runs after Testin's own log has been told to
     * stop, so a {@code Logger} call here would be queued to a writer that is
     * closing and never appear. And what it answers is a question asked of
     * {@code idea.log}: quitting the IDE stalls for about nine seconds between
     * two of the platform's own lines, and the only way to say whether this
     * method is inside that gap is to put the number in the same timeline,
     * beside {@code ComponentStoreImpl} and the rest.
     * <p>
     * At INFO and one line, so it costs a quit nothing and cannot be asked a
     * second time without an answer.
     */
    private static void report(final long elapsedNanos, final boolean timedOut) {
        IDE_LOG.info("Testin logger shutdown took " + elapsedNanos / 1_000_000 + " ms"
                + (timedOut ? ", and the writer did not finish within " + JOIN_TIMEOUT + " ms - it was interrupted"
                : ", writer finished on its own"));
    }
}
