package bai_tap_lon.client.controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * Helper UI dùng chung cho các Strategy. Không có state — chỉ static method.
 */
final class StrategyUiUtils {

    private StrategyUiUtils() {}

    static void setVisible(HBox nav, boolean show) {
        if (nav == null) return;
        nav.setVisible(show);
        nav.setManaged(show);
    }

    static Button primaryBtn(String text, EventHandler<ActionEvent> onAction) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        b.setOnAction(onAction);
        return b;
    }
}
