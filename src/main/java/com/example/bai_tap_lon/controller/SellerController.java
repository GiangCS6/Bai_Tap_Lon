package com.example.bai_tap_lon.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class SellerController {

    private String selectedImagePath;

    @FXML private TableView<Product>        tableMyItems;
    @FXML private TableColumn<Product, String> colItemName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;

    private final ObservableList<Product> products = FXCollections.observableArrayList();

    // ── Khởi tạo ────────────────────────────────────────────────
    @FXML
    public void initialize() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice   .setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus  .setCellValueFactory(new PropertyValueFactory<>("status"));
        tableMyItems.setItems(products);
    }

    // ── Đăng bán sản phẩm ───────────────────────────────────────
    @FXML
    private void handlePostItem() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm");
        dialog.setHeaderText("Nhập thông tin sản phẩm muốn đấu giá");

        ButtonType postBtn = new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postBtn, ButtonType.CANCEL);

        // Style dialog pane theo theme xanh nước biển
        dialog.getDialogPane().setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #b0cceb;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        // Fields
        TextField txtName  = new TextField();
        txtName.setPromptText("VD: Laptop Dell XPS 13");
        txtName.setStyle("-fx-background-color: #f0f6ff; -fx-border-color: #b0cceb;" +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 38;");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("VD: 15000000");
        txtPrice.setStyle("-fx-background-color: #f0f6ff; -fx-border-color: #b0cceb;" +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-pref-height: 38;");

        // Nút chọn ảnh
        Button btnChooseImage = new Button("📁  Chọn ảnh");
        btnChooseImage.setStyle(
                "-fx-background-color: #f0f6ff; -fx-border-color: #b0cceb;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-text-fill: #0277bd; -fx-cursor: hand; -fx-pref-height: 36;"
        );
        Label lblImage = new Label("Chưa chọn ảnh");
        lblImage.setStyle("-fx-text-fill: #88a8c4; -fx-font-size: 12px;");

        btnChooseImage.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Chọn ảnh sản phẩm");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
            File file = chooser.showOpenDialog(null);
            if (file != null) {
                selectedImagePath = file.toURI().toString();
                lblImage.setText(file.getName());
                lblImage.setStyle("-fx-text-fill: #0277bd; -fx-font-size: 12px;");
            }
        });

        // Layout nội dung dialog
        Label lblFieldName  = makeDialogLabel("Tên sản phẩm");
        Label lblFieldPrice = makeDialogLabel("Giá khởi điểm (VNĐ)");
        Label lblFieldImage = makeDialogLabel("Hình ảnh");

        VBox content = new VBox(8,
                lblFieldName,  txtName,
                lblFieldPrice, txtPrice,
                lblFieldImage, btnChooseImage, lblImage
        );
        content.setStyle("-fx-padding: 10 0 0 0;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(380);

        // Xử lý kết quả
        dialog.setResultConverter(button -> {
            if (button == postBtn) {
                try {
                    String name  = txtName.getText().trim();
                    double price = Double.parseDouble(txtPrice.getText().trim());
                    if (name.isEmpty()) return null;
                    return new Product(name, price, "Đang đấu giá");
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu",
                            "Giá phải là số hợp lệ. VD: 15000000");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            products.add(product);
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Sản phẩm \"" + product.getName() + "\" đã được đăng bán!");
        });
    }

    // ── Cài đặt ─────────────────────────────────────────────────
    @FXML
    private void handleSetting() {
        showAlert(Alert.AlertType.INFORMATION, "Cài đặt", "Màn hình Cài đặt đang được phát triển.");
    }

    // ── Ví tiền ─────────────────────────────────────────────────
    @FXML
    private void handleWallet() {
        showAlert(Alert.AlertType.INFORMATION, "Ví tiền", "Số dư ví: 10,000,000 VNĐ");
    }

    // ── Đăng xuất ───────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        showAlert(Alert.AlertType.INFORMATION, "Đăng xuất", "Đăng xuất thành công!");
    }

    // ── Helpers ─────────────────────────────────────────────────
    private Label makeDialogLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-text-fill: #1e3f60; -fx-font-family: 'Segoe UI Semibold', SansSerif;");
        return lbl;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ── Inner class Product ──────────────────────────────────────
    public static class Product {
        private final String name;
        private final double price;
        private final String status;

        public Product(String name, double price, String status) {
            this.name   = name;
            this.price  = price;
            this.status = status;
        }

        public String getName()   { return name;   }
        public double getPrice()  { return price;  }
        public String getStatus() { return status; }
    }
}
