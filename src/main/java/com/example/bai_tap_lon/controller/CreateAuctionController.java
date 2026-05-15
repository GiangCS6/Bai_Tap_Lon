package com.example.bai_tap_lon.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CreateAuctionController {
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField durationField;
    @FXML private Label messageLabel;

    private String username;

    public void setUsername(String username) {
        this.username = username;
    }



    //                        ========= BACK BUTTON ===========
    /*@FXML
    private void handleBack() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        AuctionListView listView = new AuctionListView(stage, username);
        listView.show();
    }*/

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
        messageLabel.setText(msg);
    }
}
