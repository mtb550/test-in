package testGit.pojo.mappers;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import testGit.pojo.GroupType;
import testGit.pojo.Priority;

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

    private String expected;

    private String steps;

    private Priority priority;

    private String autoRef;

    private String busiRef;

    private List<GroupType> groups;

    private String createBy;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;

    private String module;

}
