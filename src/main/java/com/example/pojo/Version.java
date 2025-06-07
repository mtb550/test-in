package com.example.pojo;

import lombok.Data;

@Data
public class Version {
    private int id;

    private double version;

    private String created_at;

    private String created_by;

    private String valid_from;

    private String valid_to;

    private int project_id;

    private int latest;

}
