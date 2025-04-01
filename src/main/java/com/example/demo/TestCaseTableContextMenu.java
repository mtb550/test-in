package com.example.demo;

import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestCaseTableContextMenu {
    private final JBTable table;
    private final TestCaseTableModel model;

    public TestCaseTableContextMenu(JBTable table, TestCaseTableModel model) {
        this.table = table;
        this.model = model;

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0 && !table.isRowSelected(row)) {
            table.setRowSelectionInterval(row, row);
        }

        JPopupMenu menu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem("📋 Copy");
        copyItem.addActionListener(evt -> copySelectedTestCase());

        JMenuItem runItem = new JMenuItem("▶ Run Test");
        runItem.addActionListener(evt -> runSelectedTestCase());

        JMenuItem viewItem = new JMenuItem("🔍 View Details");
        viewItem.addActionListener(evt -> viewDetails());

        menu.add(copyItem);
        menu.add(runItem);
        menu.add(viewItem);

        menu.show(table, e.getX(), e.getY());
    }

    private void copySelectedTestCase() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            TestCase tc = model.getTestCaseAt(row);
            String text = String.format("Title: %s\nSteps: %s\nExpected: %s",
                    tc.getTitle(), tc.getSteps(), tc.getExpectedResult());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }
    }

    private void runSelectedTestCase() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            TestCase tc = model.getTestCaseAt(row);
            System.out.println("Running automation for: "/* + tc.getAutomationRef()*/);
            // TODO: Actual execution logic
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            TestCase tc = model.getTestCaseAt(row);
            TestCaseToolWindow.show(tc);
        }
    }
}
