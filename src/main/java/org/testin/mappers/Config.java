package org.testin.mappers;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Config {
    public static final @NotNull DateTimeFormatter EXCEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final @NotNull String DATE_FORMAT_PATTERN = "EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'";

    @Getter
    private static final @NotNull DateTimeFormatter dateFormatterPattern = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.US);

    /**
     * Java test source root, detected once at plugin startup (see Tools.getTestSourceRoot).
     * Cached here so code generation does not re-scan the project modules on every call;
     * re-detected only if the cached root becomes invalid (e.g. the folder was removed).
     */
    @Getter
    @Setter
    private static volatile @Nullable VirtualFile testSourceRoot;

}