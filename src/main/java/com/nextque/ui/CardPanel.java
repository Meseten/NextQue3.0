package com.nextque.ui;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.JPanel;
import java.awt.LayoutManager;

public class CardPanel extends JPanel {

    public CardPanel(LayoutManager layoutManager) {
        super(layoutManager);
        stylePanel();
    }

    public CardPanel() {
        super();
        stylePanel();
    }

    private void stylePanel() {
        setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        putClientProperty(FlatClientProperties.STYLE,
            "arc: 15;" +
            "shadowType: outside;" +
            "shadowColor: #00000020;" +
            "shadowSize: 6;"
        );
    }
}
