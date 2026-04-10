package testGit.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import testGit.pojo.Groups;
import testGit.pojo.Priority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCaseDto {
    private UUID next;

    private Boolean isHead;

    private UUID id;

    ///  change to name or description to match the testng
    private String title;

    private String expected;

    private List<String> steps;

    private Priority priority;

    /// change this to PATH FCQN
    private String autoRef;

    private String busiRef;

    private List<Groups> groups;

    private String createBy;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;

    private String module;

    @JsonIgnore
    private String tempStatus;

    @JsonIgnore
    private String tempError;

}
