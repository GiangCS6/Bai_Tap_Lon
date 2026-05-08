package com.example.bai_tap_lon.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/*
public class SellerController {

    @FXML private TableView<?> tableMyItems;
    @FXML private TextArea txtLog;
    @FXML private Button btnPost;

    @FXML
    private void handlePostItem() {
        // Sau này sẽ mở một cửa sổ mới để đăng sản phẩm
        txtLog.appendText("✅ Chức năng Đăng bán sản phẩm đang được phát triển...\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng bán");
        alert.setHeaderText("Chức năng Đăng bán sản phẩm");
        alert.setContentText("Bạn muốn đăng bán món gì?\n\nTính năng này sẽ được hoàn thiện ở phần sau.");
        alert.showAndWait();
    }

}*/

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;

public class SellerController {

    private String selectedImagePath;

    @FXML private TableView<Product> tableMyItems;

    @FXML private TableColumn<Product, String> colItemName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;


    private final ObservableList<Product> products =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colItemName.setCellValueFactory(
                new PropertyValueFactory<>("name"));

        colPrice.setCellValueFactory(
                new PropertyValueFactory<>("price"));

        colStatus.setCellValueFactory(
                new PropertyValueFactory<>("status"));

        tableMyItems.setItems(products);
    }

    @FXML
    private void handlePostItem() {

        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm");

        ButtonType postButton =
                new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                postButton,
                ButtonType.CANCEL
        );

        Button btnChooseImage = new Button("Chọn ảnh");

        Label lblImage = new Label("Chưa chọn ảnh");

        btnChooseImage.setOnAction(e -> {

            FileChooser chooser = new FileChooser();

            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Images",
                            "*.png",
                            "*.jpg",
                            "*.jpeg"
                    )
            );

            File file = chooser.showOpenDialog(null);

            if (file != null) {

                selectedImagePath = file.toURI().toString();
                lblImage.setText(file.getName());
            }
        });

        TextField txtName = new TextField();
        txtName.setPromptText("Tên sản phẩm");

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Giá");

        VBox content = new VBox(10,
                new Label("Tên sản phẩm"),
                txtName,
                new Label("Giá khởi điểm"),
                txtPrice
        );

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {

            if (button == postButton) {

                try {

                    String name = txtName.getText();
                    double price =
                            Double.parseDouble(txtPrice.getText());

                    return new Product(
                            name,
                            price,
                            "Đang đấu giá"
                    );

                } catch (Exception e) {
                    return null;
                }
            }

            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            products.add(product);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Đăng bán thành công!");
            alert.showAndWait();
        });
    }

    @FXML
    private void handleSetting() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Màn hình Setting");
        alert.showAndWait();
    }

    @FXML
    private void handleWallet() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Số dư ví: 10,000,000 VNĐ");
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Đăng xuất thành công!");
        alert.showAndWait();
    }

    public static class Product {

        private String name;
        private double price;
        private String status;

        public Product(String name,
                       double price,
                       String status) {

            this.name = name;
            this.price = price;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public String getStatus() {
            return status;
        }
    }
}
