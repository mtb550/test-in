package org.testin.pojo.dto.dirs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.testin.pojo.Config;
import org.testin.pojo.CreateNodeMenu;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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

    @NonNull
    @JsonIgnore
    @Builder.Default
    private List<String> fqcn = new ArrayList<>();

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

    // todo, all dtos should has marker then apply.
    //private Marker marker;

    @NonNull
    public abstract CreateNodeMenu getMenu();
}
