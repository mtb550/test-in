package com.example.demo;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class TestCaseTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Title", "Expected Result", "Steps", "Priority"};
    private final List<TestCase> testCases;

    public TestCaseTableModel(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    @Override
    public int getRowCount() {
        return testCases.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        TestCase tc = testCases.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> tc.getTitle();
            case 1 -> tc.getExpectedResult();
            case 2 -> tc.getSteps();
            case 3 -> tc.getPriority();
            default -> "";
        };
    }

    public TestCase getTestCaseAt(int rowIndex) {
        return testCases.get(rowIndex);
    }
}
