package com.example.demo;

public class TestCase {
    private static int nextId = 1;

    private final int id;
    private final String title;
    private final String expectedResult;
    private final String steps;
    private final String priority;

    public TestCase(String title, String expectedResult, String steps, String priority) {
        this.id = nextId++;
        this.title = title;
        this.expectedResult = expectedResult;
        this.steps = steps;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public String getSteps() {
        return steps;
    }

    public String getPriority() {
        return priority;
    }
}
