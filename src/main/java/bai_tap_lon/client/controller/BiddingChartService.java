// Chứa toàn bộ logic chart

package bai_tap_lon.client.controller;

import bai_tap_lon.common.network.TimeUtil;
import javafx.collections.ObservableList;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BiddingChartService {
    private static final int MAX_CHART_POINTS = 150;
    private static final int CHART_WINDOW_SIZE = 60;
    private static final double Y_PADDING_PCT = 0.08;
    private static final double X_PADDING_PCT = 0.04;

    private static final class Point {
        final double xSec;       // epoch seconds (linear time)
        final long amount;
        Point(double xSec, long amount) { this.xSec = xSec; this.amount = amount; }
    }

    private final List<Point> allPriceData = new ArrayList<>();
    private boolean tickFormatterInstalled = false;

    public void clear(XYChart.Series<Number, Number> series, NumberAxis yAxis, Label lblChartInfo) {
        allPriceData.clear();
        series.getData().clear();
        yAxis.setAutoRanging(true);
        if (lblChartInfo != null) lblChartInfo.setVisible(false);
    }

    public void append(LocalDateTime ts, long amount, XYChart.Series<Number, Number> series,
                       NumberAxis xAxis, NumberAxis yAxis, Label lblChartInfo) {
        if (ts == null) return; // không vẽ điểm nếu không có timestamp hợp lệ
        double xSec = TimeUtil.toEpochSeconds(ts);
        // Đảm bảo X tăng đơn điệu — nếu bid mới có timestamp == hoặc sớm hơn
        // bid trước đó (do clock skew / cùng giây), nhích thêm 1ms để
        // LineChart vẫn vẽ đường nối, không bị "lùi" trục X.
        if (!allPriceData.isEmpty()) {
            double lastX = allPriceData.get(allPriceData.size() - 1).xSec;
            if (xSec <= lastX) xSec = lastX + 0.001;
        }
        allPriceData.add(new Point(xSec, amount));
        rebuild(series, xAxis, yAxis, lblChartInfo);
    }

    public void rebuild(XYChart.Series<Number, Number> series, NumberAxis xAxis, NumberAxis yAxis, Label lblChartInfo) {
        installTickFormatter(xAxis);

        if (allPriceData.isEmpty()) {
            series.getData().clear();
            yAxis.setAutoRanging(true);
            if (lblChartInfo != null) lblChartInfo.setVisible(false);
            return;
        }

        boolean windowed = allPriceData.size() > MAX_CHART_POINTS;
        int from = windowed ? Math.max(0, allPriceData.size() - CHART_WINDOW_SIZE) : 0;
        List<Point> window = allPriceData.subList(from, allPriceData.size());

        // Luôn tạo XYChart.Data MỚI — không reuse instance cũ, nếu không
        // LineChart sẽ không vẽ lại đường nối giữa các điểm.
        List<XYChart.Data<Number, Number>> fresh = new ArrayList<>(window.size());
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        long minY = Long.MAX_VALUE, maxY = Long.MIN_VALUE;
        for (Point p : window) {
            fresh.add(new XYChart.Data<>(p.xSec, p.amount));
            if (p.xSec < minX) minX = p.xSec;
            if (p.xSec > maxX) maxX = p.xSec;
            if (p.amount < minY) minY = p.amount;
            if (p.amount > maxY) maxY = p.amount;
        }

        ObservableList<XYChart.Data<Number, Number>> data = series.getData();
        data.setAll(fresh);

        applyXBounds(xAxis, minX, maxX);
        applyYBounds(yAxis, minY, maxY);

        if (lblChartInfo != null) {
            if (windowed) {
                lblChartInfo.setText("Showing last " + CHART_WINDOW_SIZE + " of " + allPriceData.size() + " bids");
                lblChartInfo.setVisible(true);
            } else {
                lblChartInfo.setVisible(false);
            }
        }
    }

    private void installTickFormatter(NumberAxis xAxis) {
        if (tickFormatterInstalled) return;
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) {
                if (n == null) return "";
                return TimeUtil.formatTimeShort(TimeUtil.fromEpochSeconds(n.doubleValue()));
            }
            @Override public Number fromString(String s) { return 0; }
        });
        tickFormatterInstalled = true;
    }

    private void applyXBounds(NumberAxis xAxis, double minVal, double maxVal) {
        // Khi chỉ có 1 điểm, tự cho khung ±30 giây để vẫn nhìn rõ.
        if (maxVal - minVal < 1.0) {
            double center = minVal;
            minVal = center - 30;
            maxVal = center + 30;
        }
        double range = maxVal - minVal;
        double padding = Math.max(range * X_PADDING_PCT, 1.0);
        double lower = minVal - padding;
        double upper = maxVal + padding;

        double tick = calcNiceTimeTickUnit(upper - lower);
        // Tick được canh theo bội số của tick unit (tính từ epoch) để
        // các nhãn HH:mm:ss rơi đúng vào mốc tròn.
        xAxis.setLowerBound(Math.floor(lower / tick) * tick);
        xAxis.setUpperBound(Math.ceil(upper / tick) * tick);
        xAxis.setTickUnit(tick);
        xAxis.setMinorTickVisible(false);
    }

    private void applyYBounds(NumberAxis yAxis, double minVal, double maxVal) {
        yAxis.setAutoRanging(false);
        double range = maxVal - minVal;
        double padding = (range < 1.0) ? Math.max(maxVal * 0.10, 1.0) : range * Y_PADDING_PCT;

        double lower = Math.max(0, minVal - padding);
        double upper = maxVal + padding;

        double tick = calcNiceTickUnit(upper - lower);
        yAxis.setLowerBound(Math.floor(lower / tick) * tick);
        yAxis.setUpperBound(Math.ceil(upper / tick) * tick);
        yAxis.setTickUnit(tick);
    }

    private double calcNiceTickUnit(double range) {
        if (range <= 0) return 1;
        double raw = range / 7.0;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double norm = raw / mag;
        double nice = (norm <= 1.5) ? 1 : (norm <= 3.5) ? 2 : (norm <= 7.5) ? 5 : 10;
        return nice * mag;
    }

    // Chọn bước thời gian "đẹp" theo tổng range (giây): 5s, 10s, 15s, 30s,
    // 1m, 2m, 5m, 10m, 15m, 30m, 1h, 2h, 6h, 12h, 24h.
    private double calcNiceTimeTickUnit(double rangeSec) {
        double[] niceSec = {
                5, 10, 15, 30,
                60, 2 * 60, 5 * 60, 10 * 60, 15 * 60, 30 * 60,
                3600, 2 * 3600, 6 * 3600, 12 * 3600, 24 * 3600
        };
        double target = rangeSec / 7.0;
        for (double s : niceSec) {
            if (s >= target) return s;
        }
        return niceSec[niceSec.length - 1];
    }
}