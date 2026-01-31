package com.example.projectPanel.testPlan;

import com.example.pojo.TestCase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestCaseCardWrapper extends JLayeredPane {

    private final JPanel overlayPanel;
    private final TestCaseCardTP card;
    private final int FIXED_HEIGHT = 100;

    public TestCaseCardWrapper(int index, TestCase testCase) {
        // 1. أهم خطوة: إلغاء الـ Layout Manager للحاوية الكبرى
        // لكي نتحكم في مكان العناصر فوق بعضها بالبكسل
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(400, FIXED_HEIGHT));

        // 2. الطبقة الخلفية (الكارت الأصلي)
        card = new TestCaseCardTP(index, testCase);
        // نضعه في الطبقة الافتراضية (رقم 0)
        add(card, JLayeredPane.DEFAULT_LAYER);

        // 3. طبقة الأوفرلاي (الأزرار العائمة)
        // نستخدم FlowLayout(RIGHT) لنجعل الأزرار في أقصى اليمين
        overlayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        overlayPanel.setOpaque(false); // تجعل اللوحة شفافة لكي نرى ما تحتها
        overlayPanel.setVisible(false); // مخفية افتراضياً

        // إضافة الأزرار للوحة الشفافة
        overlayPanel.add(createStatusButton("PASSED", new Color(40, 167, 69)));
        overlayPanel.add(createStatusButton("BLOCKED", new Color(255, 193, 7)));
        overlayPanel.add(createStatusButton("FAILED", new Color(220, 53, 69)));

        // نضع لوحة الأزرار في طبقة أعلى (رقم 100) لضمان ظهورها "فوق" النصوص
        add(overlayPanel, JLayeredPane.PALETTE_LAYER);

        // 4. منطق الاستشعار (الظهور والاختفاء المستقر)
        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                overlayPanel.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // التأكد أن الماوس خرجت فعلياً من حدود الحاوية وليس فقط فوق الأزرار
                if (!getBounds().contains(e.getPoint())) {
                    overlayPanel.setVisible(false);
                }
            }
        };

        this.addMouseListener(hoverAdapter);
        // ضروري جداً إضافة الحساس للكارت أيضاً لأن الماوس ستكون فوقه
        card.addMouseListener(hoverAdapter);
    }

    private JButton createStatusButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // نمنع الزر من "أكل" أحداث الماوس لكي لا تختفي اللوحة عند الوقوف عليه
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                overlayPanel.setVisible(true);
            }
        });

        return btn;
    }

    @Override
    public void doLayout() {
        // تحديد المواقع بدقة بكسلية (هذا يمنع اهتزاز النصوص)
        // الكارت يأخذ كامل المساحة في الخلفية
        card.setBounds(0, 0, getWidth(), getHeight());

        // الأوفرلاي يغطي الجزء العلوي فقط من الكارت
        overlayPanel.setBounds(0, 0, getWidth(), 45);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, FIXED_HEIGHT);
    }
}