package bai_tap_lon.client.controller;

import bai_tap_lon.common.network.TimeUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationCardController {
    private static final Pattern BOLD_SEGMENT = Pattern.compile("\\*\\*(.+?)\\*\\*");

    @FXML private VBox cardRoot;
    @FXML private Label titleLabel;
    @FXML private TextFlow bodyFlow;
    @FXML private Label timeLabel;

    public void setNotification(Notification notification) {
        if (notification == null) return;
        titleLabel.setText(notification.getTitle());
        timeLabel.setText(formatTime(notification.getTimestamp()));
        renderBody(notification.getBody() == null ? "" : notification.getBody());

        if (notification.isRead()) {
            cardRoot.setStyle(
                    "-fx-background-color: #0D223D; -fx-background-radius: 10; " +
                    "-fx-border-color: #1E3A5F; -fx-border-radius: 10; -fx-padding: 12;");
        } else {
            cardRoot.setStyle(
                    "-fx-background-color: #123154; -fx-background-radius: 10; " +
                    "-fx-border-color: #00CDA5; -fx-border-radius: 10; -fx-padding: 12;");
        }
    }

    private void renderBody(String rawBody) {
        bodyFlow.getChildren().clear();
        Matcher matcher = BOLD_SEGMENT.matcher(rawBody);
        int cursor = 0;
        while (matcher.find()) {
            appendNormal(rawBody.substring(cursor, matcher.start()));
            appendHighlight(matcher.group(1));
            cursor = matcher.end();
        }
        appendNormal(rawBody.substring(cursor));
    }

    private void appendNormal(String text) {
        if (text == null || text.isEmpty()) return;
        Text normal = new Text(text);
        normal.setStyle("-fx-fill: #CBD5E1; -fx-font-size: 12px;");
        bodyFlow.getChildren().add(normal);
    }

    private void appendHighlight(String text) {
        if (text == null || text.isEmpty()) return;
        Text highlight = new Text(text);
        highlight.setStyle("-fx-fill: #64FFDA; -fx-font-size: 12px; -fx-font-weight: bold;");
        bodyFlow.getChildren().add(highlight);
    }

    private String formatTime(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        return TimeUtil.formatTime(timestamp);
    }
}
