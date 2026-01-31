package com.example.editorPanel;

import com.example.pojo.TestCase;
import com.example.viewPanel.TestCaseToolWindow;
import com.intellij.openapi.ui.JBPopupMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CardMouseAdapter extends MouseAdapter {
    private final JComponent card;
    private final JBPopupMenu menu;
    private final TestCase tc; // Assuming TestCase is a type used in your project

    public CardMouseAdapter(JComponent card, JBPopupMenu menu, TestCase tc) {
        System.out.println("CardMouseAdapter.CardMouseAdapter()");
        this.card = card;
        this.menu = menu;
        this.tc = tc;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("CardMouseAdapter.mouseClicked()");
        if (SwingUtilities.isRightMouseButton(e)) {
            menu.show(card, e.getX(), e.getY());
        } else if (e.getClickCount() == 2) {
            TestCaseToolWindow.show(tc);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        System.out.println("CardMouseAdapter.mouseEntered()");
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        System.out.println("CardMouseAdapter.mouseExited()");
        card.setCursor(Cursor.getDefaultCursor());
    }
}
