package org.testin.mappers.dto.dirs;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.CreateNodeMenu;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.Config;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString()
public abstract class DirectoryDto {
    @NonNull
    @Builder.Default
    private String name = "";

    @NonNull
    @Builder.Default
    private Path path = Path.of("");

    @NonNull
    @Builder.Default
    private ArrayList<String> path2 = new ArrayList<>();

    @ToString.Exclude
    private DirectoryDto parent;

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private String createdBy = "";

    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime modifiedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    @NonNull
    @Builder.Default
    private String modifiedBy = "";

    // todo, how to make it
    //@NotNull
    //public abstract IMarker getMarker();

    @NonNull
    public abstract CreateNodeMenu getMenu();

    @Nullable
    public abstract Object resolveDirectoryObject(final Path folder, final ProjectIndexer indexer);
}
