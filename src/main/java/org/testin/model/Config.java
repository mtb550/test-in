package org.testin.model;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Config {
    public static final @NotNull DateTimeFormatter EXCEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final @NotNull String DATE_FORMAT_PATTERN = "EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'";
    /**
     * The empty timestamp: something that has not happened yet. A case nobody has
     * given a verdict, a run nobody has started - their timestamps hold this rather
     * than "now" or a null. What {@code BugSeverity.EMPTY} is to a bug that was never
     * recorded, this is to a moment that never came.
     */
    public static final @NotNull ZonedDateTime NOT_EXECUTED = Instant.EPOCH.atZone(ZoneOffset.UTC);
    @Getter
    private static final @NotNull DateTimeFormatter dateFormatterPattern = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.US);
    /**
     * Java test source root, detected once at plugin startup by JavaSourceRoot.
     * Cached here so code generation does not re-scan the project modules on
     * every call.
     */
    private static volatile @NotNull Optional<VirtualFile> testSourceRoot = Optional.empty();

    /**
     * The root already found, and empty when nothing has looked yet or when what
     * was found has since been deleted.
     * <p>
     * A cached root that stopped being valid is no answer at all, so that is
     * asked here rather than by the caller - which is where it used to live,
     * beside the null check for "nothing has looked yet" (#71).
     */
    public static @NotNull Optional<VirtualFile> testSourceRoot() {
        return testSourceRoot.filter(VirtualFile::isValid);
    }

    public static void rememberTestSourceRoot(final @NotNull VirtualFile root) {
        testSourceRoot = Optional.of(root);
    }

    /**
     * Compared as instants, not with {@code ZonedDateTime.equals}: the mapper moves
     * every timestamp it reads into the system zone, so the epoch comes back from a
     * run file as 03:00 in Asia/Riyadh and equals() would call that a different moment.
     */
    public static boolean isNotExecuted(final @NotNull ZonedDateTime at) {
        return at.toInstant().equals(NOT_EXECUTED.toInstant());
    }

}