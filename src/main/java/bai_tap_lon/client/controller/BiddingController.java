package bai_tap_lon.client.controller;

import bai_tap_lon.client.network.ServerMessageRouter;
import bai_tap_lon.common.model.entity.Auction;
import bai_tap_lon.common.network.TimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class BiddingController implements Initializable {

    // Header
    @FXML private Button btnBack;
    @FXML private Label lblItemName;
    @FXML private Label lblStatus;
    @FXML private Circle dotStatus;
    @FXML private Label lblEndTime;
    @FXML private Label lblCountdown;

    // Price panel
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblAvailableBalance;

    // Bid input
    @FXML private TextField txfBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Label lblBidMsg;

    // Auto bid
    @FXML private TextField txfMaxBid;
    @FXML private TextField txfIncrement;
    @FXML private Button btnSetAutoBid;
    @FXML private Button btnCancelAutoBid;
    @FXML private Label lblAutoBidStatus;

    // History table
    @FXML private TableView<BidRow> tblHistory;
    @FXML private TableColumn<BidRow, String> colBidder;
    @FXML private TableColumn<BidRow, String> colAmount;
    @FXML private TableColumn<BidRow, String> colTime;

    // Chart
    @FXML private LineChart<Number, Number> chart;
    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

    @FXML private Label lblChartInfo; // optional in FXML

    // State + services
    private final BiddingViewState state = new BiddingViewState();
    private final BiddingNetworkService networkService = new BiddingNetworkService();
    private final BiddingPushCoordinator pushCoordinator = new BiddingPushCoordinator();
    private final BiddingChartService chartService = new BiddingChartService();
    private final BiddingCountdownService countdownService = new BiddingCountdownService();

    private final javafx.collections.ObservableList<BidRow> bidRows = javafx.collections.FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();

    private String backFxml;
    private Timeline msgAutoHideTimeline;
    private ScaleTransition statusPulse;

    private static final Logger logger = Logger.getLogger(BiddingController.class.getName());
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupChart();

        lblBidMsg.setVisible(false);
        lblAutoBidStatus.setVisible(false);
        if (lblChartInfo != null) lblChartInfo.setVisible(false);


        btnPlaceBid.setDisable(true);
        btnSetAutoBid.setDisable(true);
        btnCancelAutoBid.setDisable(true);
        // Push subscriptions are set up in subscribePushEvents() called from initWithId()
    }

    public void initWithId(String auctionId, String backFxml) {
        if (auctionId == null || auctionId.isBlank()) {
            showBidMsg("Invalid auction id.", true);
            return;
        }

        this.state.auctionId = auctionId;
        this.backFxml = backFxml;

        subscribePushEvents();
        networkService.sendWatch(state.auctionId);
        networkService.sendGetAuctionDetail(state.auctionId, this::onGetAuctionDetailSuccess, this::onRequestFailure);
        networkService.sendGetBalance(this::onGetBalanceSuccess, (e, m) -> {});

    }

    public void initWithId(String auctionId) {
        initWithId(auctionId, "/viewfxml/active-auctions.fxml");
    }

    private void setupTable() {
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        tblHistory.setItems(bidRows);
        tblHistory.setPlaceholder(new Label("No bids yet"));
    }

    private void setupChart() {
        priceSeries.setName("Bid Price");
        chart.getData().add(priceSeries);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chartXAxis.setLabel("Time");
        chartYAxis.setLabel("Price (₫)");
        javafx.application.Platform.runLater(() -> {
            if (priceSeries.getNode() != null) {
                priceSeries.getNode().setStyle(
                        "-fx-stroke: #22d3ee; -fx-stroke-width: 2px;"
                );
            }
        });
    }

    private void subscribePushEvents() {
        pushCoordinator.subscribe(
                this::onPushBidPlaced,
                this::onPushAuctionStarted,
                this::onPushAuctionEnded,
                this::onPushTimeExtended,
                this::onPushAuctionCancelled
        );
    }

    // =============== Response handlers ===============

    private void onGetAuctionDetailSuccess(JsonObject data) {
        if (data == null || !data.has("auction") || !data.get("auction").isJsonObject()) return;

        JsonObject auction = data.getAsJsonObject("auction");

        //Chỉnh lại để hiện ItemName thay vì auctionId
        JsonObject itemObj = auction.has("item") && auction.get("item").isJsonObject()
                ? auction.getAsJsonObject("item") : null;
        String itemName = itemObj != null
                ? optString(itemObj, "name", "Auction " + state.auctionId)
                : optString(auction, "itemName", "Auction " + state.auctionId);

        state.status = optString(auction, "status", "RUNNING");
        state.currentPrice = optLong(auction, "currentPrice", 0L);
        state.endTime = parseIso(optString(auction, "endTime", null));

        lblItemName.setText(itemName);
        lblCurrentPrice.setText(formatMoney(state.currentPrice));

        //Chỉnh lại để hiện currentWinner thay vì not bid yet mỗi lần vào lại bidding
        String winnerName = optString(auction, "winnerName", null);
        boolean hasWinner = winnerName != null && !winnerName.isBlank() && !"null".equalsIgnoreCase(winnerName);
        if (hasWinner) {
            boolean finished = "FINISHED".equalsIgnoreCase(state.status) || "PAID".equalsIgnoreCase(state.status);
            lblLeader.setText((finished ? "Winner: " : "Leading: ") + winnerName);
        } else {
            lblLeader.setText("No bids yet");
        }

        lblEndTime.setText("Ends: " + (state.endTime != null ? state.endTime.format(DT_FMT) : "--/-- --:--"));
        applyStatus(state.status);

        boolean running = "RUNNING".equalsIgnoreCase(state.status);
        boolean open = "OPEN".equalsIgnoreCase(state.status);
        btnPlaceBid.setDisable(!running);
        btnSetAutoBid.setDisable(!running || open);
        btnCancelAutoBid.setDisable(!running || open);

        if (running) {
            countdownService.start(lblCountdown, () -> state.endTime);
        } else {
            countdownService.stop();
            lblCountdown.setText(statusDisplayText(state.status));
        }

        if (data.has("bidHistory") && data.get("bidHistory").isJsonArray()) {
            populateHistory(data.getAsJsonArray("bidHistory"));
        }

        //pre fill autobid nếu bidder đã set t trước
        if (data.has("myAutoBid") && data.get("myAutoBid").isJsonObject()) {
            JsonObject ab = data.getAsJsonObject("myAutoBid");
            long maxBid   = optLong(ab, "maxBid",    0L);
            long increment = optLong(ab, "increment", 0L);
            if (maxBid > 0 && increment > 0) {
                txfMaxBid.setText(String.valueOf(maxBid));
                txfIncrement.setText(String.valueOf(increment));
                state.autoBidActive = true;
                lblAutoBidStatus.setText("Auto Bid: ON");
                lblAutoBidStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12;");
                lblAutoBidStatus.setVisible(true);
            }
        }
    }


    private void onGetBidHistorySuccess(JsonObject data) {
        if (data == null || !data.has("bids") || !data.get("bids").isJsonArray()) return;
        populateHistory(data.getAsJsonArray("bids"));
    }


    private void onGetBalanceSuccess(JsonObject data) {
        if (data == null || !data.has("balance")) return;
        state.availableBalance = data.get("balance").getAsLong();
        lblAvailableBalance.setText("Available Balance: " + formatMoney(state.availableBalance));
    }

    private void onPlaceBidSuccess(JsonObject data) {
        btnPlaceBid.setDisable(false);
        txfBidAmount.clear();

        long newPrice = optLong(data, "newPrice", state.currentPrice);
        state.currentPrice = newPrice;
        lblCurrentPrice.setText(formatMoney(newPrice));
        animatePriceFlash();
        showBidMsg("Bid placed successfully: " + formatMoney(newPrice), false);

//        if (data != null && data.has("transaction") && data.get("transaction").isJsonObject()) {
//            JsonObject tx = data.getAsJsonObject("transaction");
//            String bidder = optString(tx, "bidderName", "Unknown");
//            long amount = optLong(tx, "amount", newPrice);
//            LocalDateTime ts = parseIso(optString(tx, "timestamp", null));
//
//            appendBidRowInternal(bidder, amount, ts);
//            lblLeader.setText("Leading: " + bidder);
//        }

        boolean extended = data != null
                && data.has("timeExtended")
                && !data.get("timeExtended").isJsonNull()
                && data.get("timeExtended").getAsBoolean();

        if (extended && data.has("newEndTime") && !data.get("newEndTime").isJsonNull()) {
            state.endTime = parseIso(data.get("newEndTime").getAsString());
            lblEndTime.setText("Ends: " + (state.endTime != null ? state.endTime.format(DT_FMT) : "--/-- --:--"));
            countdownService.start(lblCountdown, () -> state.endTime);
        }

        networkService.sendGetBalance(this::onGetBalanceSuccess, (e, m) -> {});
    }

    private void onPlaceBidFailure(String err, String msg) {
        btnPlaceBid.setDisable(false);
        String text = (msg != null && !msg.isBlank()) ? msg : err;
        showBidMsg(text != null ? text : "Place bid failed.", true);
    }

    private void onSetAutoBidSuccess(JsonObject data) {
        btnSetAutoBid.setDisable(false);
        state.autoBidActive = true;
        lblAutoBidStatus.setText("Auto Bid: ON");
        lblAutoBidStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12;");
        lblAutoBidStatus.setVisible(true);
        showBidMsg("Auto bid enabled.", false);
    }

    private void onCancelAutoBidSuccess(JsonObject data) {
        btnCancelAutoBid.setDisable(false);
        state.autoBidActive = false;
        lblAutoBidStatus.setText("Auto Bid: OFF");
        lblAutoBidStatus.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12;");
        lblAutoBidStatus.setVisible(true);
        txfMaxBid.clear();
        txfIncrement.clear();
        showBidMsg("Auto bid cancelled.", false);
    }

    private void onRequestFailure(String err, String msg) {
        String text = (msg != null && !msg.isBlank()) ? msg : err;
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Request failed");
        a.setContentText(text != null ? text : "Unknown error");
        a.showAndWait();
    }

    // =============== Push handlers ===============

    private void onPushBidPlaced(JsonObject data) {

//        Platform.runLater();

        if (!matchesThisAuction(data)) return;

        String bidder = optString(data, "bidderName", "Unknown");
        long amount = optLong(data, "amount", 0L);
        String ts = optString(data, "timestamp", null);

        if (data.has("transaction") && data.get("transaction").isJsonObject()) {
            JsonObject tx = data.getAsJsonObject("transaction");
            bidder = optString(tx, "bidderName", bidder);
            amount = optLong(tx, "amount", amount);
            ts = optString(tx, "timestamp", ts);
        }

        appendBidRowInternal(bidder, amount, parseIso(ts));
        state.currentPrice = amount;
        lblCurrentPrice.setText(formatMoney(amount));
        lblLeader.setText("Leading: " + bidder);
        animatePriceFlash();

        networkService.sendGetBalance(this::onGetBalanceSuccess, (e, m) -> {});
    }

    private void onPushAuctionStarted(JsonObject data) {
        if (!matchesThisAuction(data)) return;

        state.status = "RUNNING";
        state.endTime = parseIso(optString(data, "endTime", null));

        applyStatus(state.status);
        if (state.endTime != null) lblEndTime.setText("Ends: " + state.endTime.format(DT_FMT));

        btnPlaceBid.setDisable(false);
        btnSetAutoBid.setDisable(false);
        btnCancelAutoBid.setDisable(false);

        countdownService.start(lblCountdown, () -> state.endTime);
        showBidMsg("Auction has started!", false);
    }

    private void onPushAuctionEnded(JsonObject data) {
        if (!matchesThisAuction(data)) return;

        state.status = optString(data, "status", "FINISHED");
        long finalPrice = optLong(data, "finalPrice", state.currentPrice);
        String winner = optString(data, "winnerName", null);

        countdownService.stop();
        lblCountdown.setText("Finished");
        lblCountdown.setStyle("-fx-text-fill: #94a3b8;");
        applyStatus(state.status);
        disableBidControls();

        state.currentPrice = finalPrice;
        lblCurrentPrice.setText(formatMoney(finalPrice));

        if (winner != null && !winner.isBlank() && !"null".equalsIgnoreCase(winner)) {
            showBidMsg("Ended — " + winner + " won with " + formatMoney(finalPrice), false);
            lblLeader.setText("Winner: " + winner);
        } else {
            showBidMsg("Auction ended with no bids.", true);
        }
    }

    private void onPushTimeExtended(JsonObject data) {
        if (!matchesThisAuction(data)) return;

        state.endTime = parseIso(optString(data, "newEndTime", null));
        if (state.endTime != null) lblEndTime.setText("Ends: " + state.endTime.format(DT_FMT));

        countdownService.start(lblCountdown, () -> state.endTime);
        showBidMsg("Auction time extended!", false);
    }

    private void onPushAuctionCancelled(JsonObject data) {
        if (!matchesThisAuction(data)) return;

        state.status = "CANCELED";
        String reason = optString(data, "reason", "");

        countdownService.stop();
        lblCountdown.setText("Canceled");
        lblCountdown.setStyle("-fx-text-fill: #ef4444;");
        applyStatus(state.status);
        disableBidControls();

        showBidMsg("Auction canceled: " + reason, true);
    }

    // =============== FXML actions ===============

    @FXML
    private void onPlaceBid() {
        String raw = txfBidAmount.getText() == null ? "" : txfBidAmount.getText().trim();
        raw = raw.replace(",", "").replace(".", "").replace(" ", "");
        if (raw.isEmpty()) {
            showBidMsg("Please enter an amount.", true);
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            showBidMsg("Invalid amount.", true);
            return;
        }

        if (amount <= state.currentPrice) {
            showBidMsg("Bid must be higher than current price: " + formatMoney(state.currentPrice), true);
            return;
        }
        if (state.availableBalance > 0 && amount > state.availableBalance) {
            showBidMsg("Insufficient balance. Available: " + formatMoney(state.availableBalance), true);
            return;
        }
        if (!"RUNNING".equalsIgnoreCase(state.status)) {
            showBidMsg("Auction is not running.", true);
            return;
        }

        btnPlaceBid.setDisable(true);
        networkService.sendPlaceBid(state.auctionId, amount, this::onPlaceBidSuccess, this::onPlaceBidFailure);
    }

    @FXML
    private void onSetAutoBid() {
        String rawMax = txfMaxBid.getText() == null ? "" : txfMaxBid.getText().trim();
        String rawInc = txfIncrement.getText() == null ? "" : txfIncrement.getText().trim();

        rawMax = rawMax.replace(",", "").replace(".", "").replace(" ", "");
        rawInc = rawInc.replace(",", "").replace(".", "").replace(" ", "");

        if (rawMax.isEmpty() || rawInc.isEmpty()) {
            showBidMsg("Please fill in max price and increment.", true);
            return;
        }

        long maxBid;
        long increment;
        try {
            maxBid = Long.parseLong(rawMax);
            increment = Long.parseLong(rawInc);
        } catch (NumberFormatException ex) {
            showBidMsg("Invalid number.", true);
            return;
        }

        if (increment <= 0) {
            showBidMsg("Increment must be positive.", true);
            return;
        }
        if (maxBid < state.currentPrice + increment) {
            showBidMsg("maxBid must be >= currentPrice + increment (" + formatMoney(state.currentPrice + increment) + ")", true);
            return;
        }

        btnSetAutoBid.setDisable(true);
        networkService.sendSetAutoBid(
                state.auctionId,
                maxBid,
                increment,
                this::onSetAutoBidSuccess,
                (err, msg) -> {
                    btnSetAutoBid.setDisable(false);
                    showBidMsg(msg != null ? msg : "Cannot enable auto bid.", true);
                }
        );
    }

    @FXML
    private void onCancelAutoBid() {
        if (!state.autoBidActive) {
            showBidMsg("No active auto bid to cancel.", true);
            return;
        }
        btnCancelAutoBid.setDisable(true);
        networkService.sendCancelAutoBid(
                state.auctionId,
                this::onCancelAutoBidSuccess,
                (err, msg) -> {
                    btnCancelAutoBid.setDisable(false);
                    showBidMsg(msg != null ? msg : "Cannot cancel auto bid.", true);
                }
        );
    }

    @FXML
    private void onBack() {
        countdownService.stop();
        stopStatusPulse();
        pushCoordinator.unsubscribe();
        networkService.sendUnwatch(state.auctionId);

        Stage stage = (Stage) lblItemName.getScene().getWindow();

        String path = (backFxml != null && !backFxml.isBlank())
                ? backFxml
                : "/viewfxml/active-auctions.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Cannot go back: " + ex.getMessage());
            a.showAndWait();
        }
    }

    // =============== UI helpers ===============

    private void populateHistory(JsonArray arr) {
        bidRows.clear();
        chartService.clear(priceSeries, chartYAxis, lblChartInfo);
        if (arr == null) return;

        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject b = el.getAsJsonObject();
            String bidder = optString(b, "bidderName", "Unknown");
            long amount = optLong(b, "amount", 0L);
            LocalDateTime ts = parseIso(optString(b, "timestamp", null));
            logger.info("History bid: " + bidder + " - " + amount + " at " + ts);
            appendBidRowInternal(bidder, amount, ts);
        }

        if (!bidRows.isEmpty()) tblHistory.scrollTo(bidRows.size() - 1);
    }

    private void appendBidRowInternal(String bidder, long amount, LocalDateTime ts) {
        bidRows.add(new BidRow(bidder, formatMoney(amount), TimeUtil.formatTimeShort(ts)));
        chartService.append(ts, amount, priceSeries, chartXAxis, chartYAxis, lblChartInfo);
        tblHistory.scrollTo(bidRows.size() - 1);
    }

    private void applyStatus(String st) {
        if (st == null) st = "";
        lblStatus.setText(st.toUpperCase());

        switch (st.toUpperCase()) {
            case "RUNNING" -> {
                dotStatus.setFill(Color.web("#22c55e"));
                startPulseDot();
            }
            case "OPEN" -> {
                stopStatusPulse();
                dotStatus.setFill(Color.web("#f59e0b"));
            }
            case "FINISHED" -> {
                stopStatusPulse();
                dotStatus.setFill(Color.web("#818cf8"));
            }
            case "CANCELED" -> {
                stopStatusPulse();
                dotStatus.setFill(Color.web("#ef4444"));
            }
            case "PAID" -> {
                stopStatusPulse();
                dotStatus.setFill(Color.web("#38bdf8"));
            }
            default -> {
                stopStatusPulse();
                dotStatus.setFill(Color.web("#64748b"));
            }
        }
    }

    private String statusDisplayText(String st) {
        if (st == null) return "--:--:--";
        return switch (st.toUpperCase()) {
            case "OPEN" -> "Not started";
            case "FINISHED" -> "Finished";
            case "PAID" -> "Paid";
            case "CANCELED" -> "Canceled";
            default -> "--:--:--";
        };
    }

    private void disableBidControls() {
        btnPlaceBid.setDisable(true);
        btnSetAutoBid.setDisable(true);
        btnCancelAutoBid.setDisable(true);
    }

    private void animatePriceFlash() {
        lblCurrentPrice.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 38; -fx-font-weight: bold;");
        new Timeline(new KeyFrame(Duration.millis(700),
                e -> lblCurrentPrice.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 38; -fx-font-weight: bold;")
        )).play();
    }

    private void startPulseDot() {
        if (statusPulse != null) {
            statusPulse.stop();
        }
        dotStatus.setScaleX(1.0);
        dotStatus.setScaleY(1.0);

        statusPulse = new ScaleTransition(Duration.millis(700), dotStatus);
        statusPulse.setFromX(1);
        statusPulse.setToX(1.6);
        statusPulse.setFromY(1);
        statusPulse.setToY(1.6);
        statusPulse.setAutoReverse(true);
        statusPulse.setCycleCount(Animation.INDEFINITE);
        statusPulse.play();
    }

    private void stopStatusPulse() {
        if (statusPulse != null) {
            statusPulse.stop();
            statusPulse = null;
        }
        dotStatus.setScaleX(1.0);
        dotStatus.setScaleY(1.0);
    }

    private void showBidMsg(String msg, boolean isError) {
        lblBidMsg.setText(msg);
        lblBidMsg.setStyle("-fx-text-fill: " + (isError ? "#ef4444" : "#22c55e") + "; -fx-font-size: 12;");
        lblBidMsg.setVisible(true);

        if (msgAutoHideTimeline != null) msgAutoHideTimeline.stop();
        msgAutoHideTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> lblBidMsg.setVisible(false)));
        msgAutoHideTimeline.play();
    }

    // =============== Parsing helpers ===============

    private boolean matchesThisAuction(JsonObject data) {
        if (data == null || state.auctionId == null) return false;
        if (!data.has("auctionId") || data.get("auctionId").isJsonNull()) return false;
        return state.auctionId.equals(data.get("auctionId").getAsString());
    }

    private static String optString(JsonObject o, String key, String def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        try { return o.get(key).getAsString(); } catch (Exception e) { return def; }
    }

    private static long optLong(JsonObject o, String key, long def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return def;
        try { return o.get(key).getAsLong(); } catch (Exception e) { return def; }
    }

    private static LocalDateTime parseIso(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return TimeUtil.fromIso(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String formatMoney(long amount) {
        return String.format("%,d ₫", amount);
    }

    // DTO
    public static class BidRow {
        private final String bidder;
        private final String amount;
        private final String time;

        public BidRow(String bidder, String amount, String time) {
            this.bidder = bidder;
            this.amount = amount;
            this.time = time;
        }

        public String getBidder() { return bidder; }
        public String getAmount() { return amount; }
        public String getTime() { return time; }
    }
}