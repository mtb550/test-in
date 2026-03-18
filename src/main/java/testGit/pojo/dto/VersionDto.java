package testGit.pojo.dto;

import lombok.Data;

@Data
public class VersionDto {
    private int id;

    private double version;

    private String created_at;

    private String created_by;

    private String valid_from;

    private String valid_to;

    private int project_id;

    private int latest;

}
