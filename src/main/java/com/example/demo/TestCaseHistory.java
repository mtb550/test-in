package com.example.demo;

public class TestCaseHistory {
    private final String timestamp;
    private final String changeSummary;

    public TestCaseHistory(String timestamp, String changeSummary) {
        this.timestamp = timestamp;
        this.changeSummary = changeSummary;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getChangeSummary() {
        return changeSummary;
    }
}
