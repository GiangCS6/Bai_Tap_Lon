//Chứa countdown timeline: start/stop/tick, đổi màu theo thời gian còn lại.

package bai_tap_lon.client.controller;

import bai_tap_lon.common.network.TimeUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class BiddingCountdownService {
    private Timeline countdown;

    public void start(Label lblCountdown, Supplier<LocalDateTime> endTimeSupplier) {
        stop();
        if (endTimeSupplier.get() == null) return;

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick(lblCountdown, endTimeSupplier.get())));
        countdown.setCycleCount(Animation.INDEFINITE);
        countdown.play();
        tick(lblCountdown, endTimeSupplier.get());
    }

    public void stop() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
    }

    private void tick(Label lblCountdown, LocalDateTime endTime) {
        if (endTime == null) {
            lblCountdown.setText("--:--:--");
            return;
        }
        long secs = TimeUtil.now().until(endTime, ChronoUnit.SECONDS);
        if (secs <= 0) {
            lblCountdown.setText("00:00:00");
            stop();
            return;
        }
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
        String color = secs <= 60 ? "#ef4444" : secs <= 300 ? "#f59e0b" : "#f8fafc";
        lblCountdown.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}