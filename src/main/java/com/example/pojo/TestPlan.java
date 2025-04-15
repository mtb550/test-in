package com.example.pojo;

import lombok.Data;

@Data
public class TestPlan {
    private int id;
    private String name;
    private int type;
    private int link;
    private int project_id; // this was missing from Tree
    private String created_by;
    private String created_at;
}
