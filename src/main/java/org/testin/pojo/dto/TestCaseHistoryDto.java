package org.testin.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@ToString()
@Builder
public class TestCaseHistoryDto {
    @NotNull
    @Builder.Default
    private String timestamp = "";

    @NotNull
    @Builder.Default
    private String changeSummary = "";
}
