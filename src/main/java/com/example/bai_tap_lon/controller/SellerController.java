package com.example.bai_tap_lon.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SellerController {

    @FXML private TableView<?> tableMyItems;
    @FXML private TextArea txtLog;

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
}