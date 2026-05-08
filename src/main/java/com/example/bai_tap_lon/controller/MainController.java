package com.example.bai_tap_lon.controller;

import com.example.bai_tap_lon.client.ClientHandler;
import com.example.bai_tap_lon.model.Product;
import com.example.bai_tap_lon.service.ProductStore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class MainController implements ClientHandler.AuctionListener {

    @FXML private ListView<Product> listProducts;
    @FXML private TableView<AuctionItem> tableItems;
    @FXML private TableColumn<AuctionItem, String> colItemName;
    @FXML private TableColumn<AuctionItem, String> colCurrentPrice;
    @FXML private TableColumn<AuctionItem, String> colHighestBidder;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private TextArea txtLog;

    private ClientHandler clientHandler;
    private final ObservableList<AuctionItem> itemList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Thiết lập bảng
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colHighestBidder.setCellValueFactory(new PropertyValueFactory<>("highestBidder"));
        tableItems.setItems(itemList);

        appendLog("🚀 Đang kết nối đến Server đấu giá...");
        // Tự động kết nối Server khi mở chương trình
        connectToServer();

        //

    }

    private void connectToServer() {
        try {
            // Kết nối với Server (không cần login nữa)
            clientHandler = new ClientHandler("127.0.0.1", 8080, this);
            Thread thread = new Thread(clientHandler);
            thread.setDaemon(true);
            thread.start();

            appendLog("✅ Đã kết nối Server. Đang tải danh sách món đấu giá...");

            // Tự động yêu cầu danh sách món ngay khi kết nối
            // (sẽ gọi sau khi ClientHandler kết nối thành công)

        } catch (IOException e) {
            appendLog("❌ Không thể kết nối đến Server: " + e.getMessage());
            btnBid.setDisable(true);
        }
    }


    @FXML
    private void handleBid() {
        AuctionItem selected = tableItems.getSelectionModel().getSelectedItem();
        if (selected == null) {
            appendLog("❌ Vui lòng chọn một món đấu giá trong bảng!");
            return;
        }

        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            appendLog("❌ Vui lòng nhập số tiền đấu giá!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            clientHandler.bid(selected.getName(), amount);
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            appendLog("❌ Số tiền không hợp lệ!");
        }
    }

    // ====================== CALLBACK TỪ CLIENTHANDLER ======================
    @Override
    public void onLoginSuccess(String username) {
        // Không dùng login nữa, nhưng giữ lại để tương thích với ClientHandler
        appendLog("✅ Đã kết nối thành công với Server đấu giá.");
    }

    @Override
    public void onLoginFailed(String message) {
        appendLog("❌ Kết nối thất bại: " + message);
    }

    @Override
    public void onItemListReceived(String rawList) {
        itemList.clear();
        String[] items = rawList.split(";");
        for (String itemStr : items) {
            if (itemStr.trim().isEmpty()) continue;
            String[] data = itemStr.split("\\|");
            if (data.length == 3) {
                String name = data[0];
                String price = String.format("%,.0f VNĐ", Double.parseDouble(data[1]));
                String bidder = data[2];
                itemList.add(new AuctionItem(name, price, bidder));
            }
        }
        appendLog("📋 Đã tải " + itemList.size() + " món đấu giá.");
    }

    @Override
    public void onNewBid(String itemName, double price, String bidder) {
        for (AuctionItem item : itemList) {
            if (item.getName().equals(itemName)) {
                item.setPrice(String.format("%,.0f VNĐ", price));
                item.setHighestBidder(bidder);
                break;
            }
        }
        tableItems.refresh();
        appendLog("🔥 " + bidder + " vừa đấu giá " + String.format("%,.0f VNĐ", price) + " cho " + itemName);
    }

    @Override
    public void onSystemMessage(String message) {
        appendLog("📢 " + message);
    }

    @Override
    public void onError(String message) {
        appendLog("❌ " + message);
    }

    @Override
    public void onConnectionClosed() {
        appendLog("🔴 Kết nối với Server đã bị ngắt.");
        btnBid.setDisable(true);
    }

    private void appendLog(String text) {
        Platform.runLater(() -> txtLog.appendText(text + "\n"));
    }

    // Model cho TableView
    public static class AuctionItem {
        private String name;
        private String price;
        private String highestBidder;

        public AuctionItem(String name, String price, String highestBidder) {
            this.name = name;
            this.price = price;
            this.highestBidder = highestBidder;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public String getHighestBidder() { return highestBidder; }
        public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }
    }

    // =============================== NAVIGATION BAR ==================================
    @FXML
    private void handleSetting() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Màn hình Setting");
        alert.showAndWait();
    }

    @FXML
    private void handleWallet() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Số dư ví: 5,000,000 VNĐ");
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Đăng xuất thành công!");
        alert.showAndWait();
    }

}