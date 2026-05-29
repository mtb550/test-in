package org.testin.pojo.dto.dirs;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String name;

    private Path path;

    private ArrayList<String> path2;

    private List<String> fqcn;

    @ToString.Exclude
    private DirectoryDto parent;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    private String createdBy;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Config.DATE_FORMAT_PATTERN, locale = "en_US")
    private ZonedDateTime modifiedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);


    private String modifiedBy;

    public abstract CreateNodeMenu getMenu();
}
