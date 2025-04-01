package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestCase {
    private  String id;
    private  String title;
    private  String expectedResult;
    private  String steps;
    private  String priority;

}
