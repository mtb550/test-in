package org.testin.pojo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@Data
@ToString()
@SuperBuilder
public class VersionDto {
    @NotNull
    @Builder.Default
    private Integer id = -1;

    @NotNull
    @Builder.Default
    private Double version = -1.0;

    @NotNull
    @Builder.Default
    private String created_at = "";

    @NotNull
    @Builder.Default
    private String created_by = "";

    @NotNull
    @Builder.Default
    private String valid_from = "";

    @NotNull
    @Builder.Default
    private String valid_to = "";

    @NotNull
    @Builder.Default
    private Integer project_id = -1;

    @NotNull
    @Builder.Default
    private Integer latest = -1;

}
