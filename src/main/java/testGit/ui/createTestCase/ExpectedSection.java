package testGit.ui.createTestCase;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Set;

public class ExpectedSection implements CreateTestCaseSection {
    private final ExtendableTextField expectedField;
    private final JPanel wrapper;
    Font fieldFont = JBFont.regular().deriveFont(JBUI.Fonts.label().getSize2D() + 2f);

    public ExpectedSection() {
        this.expectedField = new ExtendableTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && hasFocus()) {
                    try {
                        Rectangle2D r = modelToView2D(0);
                        if (r != null) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIUtil.getContextHelpForeground());
                            g2.setFont(getFont());
                            FontMetrics fm = g2.getFontMetrics();

                            int x = (int) r.getX() + JBUI.scale(1);
                            int y = (int) r.getY() + fm.getAscent();

                            g2.drawString(CreateField.EXPECTED.getLabel(), x, y);
                            g2.dispose();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        this.expectedField.setFont(fieldFont);
        this.expectedField.getEmptyText().setFont(fieldFont);
        this.expectedField.getEmptyText().setText(CreateField.EXPECTED.getLabel());
        this.expectedField.setBorder(JBUI.Borders.empty(10));

        this.expectedField.setExtensions(new ExtendableTextComponent.Extension() {
            @Override
            public Icon getIcon(boolean hovered) {
                return CreateField.EXPECTED.getIcon();
            }

            @Override
            public boolean isIconBeforeText() {
                return true;
            }

            @Override
            public int getIconGap() {
                return JBUI.scale(8);
            }
        });

        this.wrapper = new JPanel(new BorderLayout());
        this.wrapper.setOpaque(false);
        this.wrapper.add(this.expectedField, BorderLayout.CENTER);
        this.wrapper.setBorder(JBUI.Borders.emptyTop(8));
    }

    @Override
    public JPanel getWrapper() {
        return wrapper;
    }

    @Override
    public void showSection(JPanel contentPanel) {
        if (wrapper.getParent() == null)
            contentPanel.add(wrapper);
        expectedField.requestFocus();
    }

    @Override
    public void applyTo(TestCaseDto dto) {
        if (wrapper.getParent() != null) {
            dto.setExpected(expectedField.getText().trim());
        }
    }

    @Override
    public void setupShortcut(final JComponent mainPanel, final JPanel slot, final CreateTestCaseBase base, final CreateTestCaseBase.UIAction repackAction, final Set<String> uniqueStepsCache) {
        base.registerShortcut(mainPanel, KeyboardSet.CreateTestCaseExpected.getShortcut(), () -> {
            showSection(slot);
            repackAction.execute();
        });
    }

    @Override
    public JComponent getFocusComponent() {
        return expectedField;
    }
}