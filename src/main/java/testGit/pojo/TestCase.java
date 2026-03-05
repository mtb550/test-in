package testGit.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {

    private int uid;

    private List<String> path;

    private UUID next;

    private Boolean isHead;

    private String id;

    private String title;

    private String expectedResult;

    private String steps;

    private Priority priority;

    private String automationRef;

    private String businessRef;

    private List<GroupType> groups;

    private String createBy;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;

    private String module;

}
